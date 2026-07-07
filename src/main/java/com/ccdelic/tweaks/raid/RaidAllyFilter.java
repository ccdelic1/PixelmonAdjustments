package com.ccdelic.tweaks.raid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ccdelic.tweaks.CCDelicTweaksMod;
import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.pixelmonmod.pixelmon.api.events.raids.RandomizeRaidAllyEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.pokemon.species.stat.ImmutableBattleStats;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.pokemon.type.Type;
import com.pixelmonmod.pixelmon.api.util.WeightedSet;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import com.pixelmonmod.pixelmon.battles.raids.ally.RaidAlly;

import net.minecraft.core.Holder;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Smart Raid Ally Selection (Feature #4).
 *
 * <p>When a solo raid needs AI partners, Pixelmon fires
 * {@link RandomizeRaidAllyEvent.SelectAlliesEvent} with the weighted pool of candidate allies before
 * one is drawn. This listener trims that pool so partners are appropriate:
 * <ol>
 *   <li><b>Evolution stage:</b> exclude first-stage (unevolved-but-evolvable) Pokemon, so partners
 *       are only single-stage, middle, or fully-evolved.</li>
 *   <li><b>Type safety:</b> exclude allies that the raid boss's type(s) hit super-effectively.</li>
 *   <li><b>BST window:</b> keep only allies whose Base Stat Total is within
 *       {@code [min(playerBST, raidBST), max(playerBST, raidBST) + range]}. The lower bound is a hard
 *       floor -- partners are never weaker than the weaker of the player/boss -- so only the upper
 *       bound carries the {@code range}. If no ally fits, the range widens to 100, then +50 per step
 *       (raising only the ceiling) until at least one ally qualifies.</li>
 * </ol>
 *
 * <p>The evolution and type passes fall back to their pre-pass pool if they would empty it, and the
 * BST expansion guarantees a non-empty result whenever any ally exists, so a raid never ends up with
 * zero candidates (which would make {@code getRandomAlly()} return null). The whole listener is
 * wrapped in a try/catch: any failure leaves the original pool untouched rather than breaking a raid.
 *
 * <p>{@code WeightedSet} has no {@code removeIf}; we snapshot, filter into a list, then rebuild the
 * set with {@code clear()} + {@code add()} preserving each ally's original weight.
 */
public class RaidAllyFilter {

    /** Widest +/- BST range tried before giving up expansion (covers the full realistic BST spread). */
    private static final int BST_RANGE_CAP = 1000;

    /** Cache of ally spec string -> spec-stable stats (BST + evolution stage), to avoid re-parsing. */
    private static final ConcurrentMap<String, CachedStats> STATS_CACHE = new ConcurrentHashMap<>();

    /** Spec-stable data cached per ally spec string (weakness is per-raid, so it is not cached). */
    private record CachedStats(int bst, boolean firstStage) {
    }

    /** An ally paired with everything we computed about it in a single spec resolution. */
    private record AllyInfo(RaidAlly ally, double weight, int bst, boolean weakToBoss, boolean firstStage) {
    }

    @SubscribeEvent
    public void onSelectAllies(RandomizeRaidAllyEvent.SelectAlliesEvent event) {
        try {
            if (!CCDelicTweaksConfig.raidAllyEnabled()) {
                return;
            }

            WeightedSet<RaidAlly> allies = event.getAllies();
            if (allies == null || allies.isEffectivelyEmpty()) {
                return;
            }

            RaidData raid = event.getRaid();
            if (raid == null) {
                return;
            }

            int playerBst = playerBst(raid);
            Pokemon boss = raid.getPokemon();
            int raidBst = boss == null ? -1 : safeBst(boss.getForm());
            if (playerBst <= 0 || raidBst <= 0) {
                return; // Missing data -- leave selection untouched.
            }

            boolean avoidWeakness = CCDelicTweaksConfig.raidAllyAvoidWeakness();
            boolean excludeFirstStage = CCDelicTweaksConfig.raidAllyExcludeFirstStage();
            List<Holder<Type>> bossTypes = avoidWeakness ? boss.getForm().getTypes() : List.of();

            // Single resolution pass: compute BST, evolution stage, and (if needed) weakness per ally.
            List<AllyInfo> infos = new ArrayList<>();
            for (Iterator<RaidAlly> it = allies.iterator(); it.hasNext(); ) {
                RaidAlly ally = it.next();
                if (ally != null) {
                    infos.add(resolve(ally, avoidWeakness, bossTypes));
                }
            }
            if (infos.isEmpty()) {
                return;
            }

            // Pass 1: exclude first-stage Pokemon (fall back to the full set if none would remain).
            List<AllyInfo> pool = infos;
            int removedByStage = 0;
            if (excludeFirstStage) {
                List<AllyInfo> notFirst = new ArrayList<>();
                for (AllyInfo info : pool) {
                    if (!info.firstStage()) {
                        notFirst.add(info);
                    }
                }
                if (!notFirst.isEmpty()) {
                    removedByStage = pool.size() - notFirst.size();
                    pool = notFirst;
                }
            }

            // Pass 2: exclude allies weak to the raid boss (fall back to the pre-pass pool if none remain).
            int removedByType = 0;
            if (avoidWeakness) {
                List<AllyInfo> notWeak = new ArrayList<>();
                for (AllyInfo info : pool) {
                    if (!info.weakToBoss()) {
                        notWeak.add(info);
                    }
                }
                if (!notWeak.isEmpty()) {
                    removedByType = pool.size() - notWeak.size();
                    pool = notWeak;
                }
            }

            // Pass 3: BST window. The lower bound is a HARD FLOOR at the weaker of the player/boss BST
            // (partners are never weaker than that); only the upper bound carries the +range and widens
            // on fallback until at least one ally qualifies.
            int lo = Math.min(playerBst, raidBst);
            int hi = Math.max(playerBst, raidBst);
            List<AllyInfo> finalAllies = null;
            int usedRange = -1;
            for (int range : rangeSequence(CCDelicTweaksConfig.raidAllyBstRange())) {
                int upper = hi + range;
                List<AllyInfo> selected = new ArrayList<>();
                for (AllyInfo info : pool) {
                    if (info.bst() >= lo && info.bst() <= upper) {
                        selected.add(info);
                    }
                }
                if (!selected.isEmpty()) {
                    finalAllies = selected;
                    usedRange = range;
                    break;
                }
            }
            if (finalAllies == null) {
                // No parseable ally fit even the widest window -- keep the current pool as a safety net.
                finalAllies = pool;
            }

            // Nothing was removed overall? Leave the set untouched.
            if (finalAllies.size() == infos.size()) {
                return;
            }

            // Rebuild the weighted set with the survivors, preserving original weights.
            allies.clear();
            for (AllyInfo info : finalAllies) {
                allies.add(info.weight(), info.ally());
            }

            if (CCDelicTweaksConfig.raidAllyLogFiltered()) {
                CCDelicTweaksMod.LOGGER.info(
                        "[PixelmonAdjustments] Raid ally filter (player BST {}, boss BST {}): removed {} first-stage, "
                                + "{} type-weak; BST range +/-{} -> {} of {} allies remain.",
                        playerBst, raidBst, removedByStage, removedByType, usedRange, finalAllies.size(), infos.size());
            }
        } catch (Throwable t) {
            CCDelicTweaksMod.LOGGER.error("[PixelmonAdjustments] Raid ally filter error; leaving ally pool unchanged.", t);
        }
    }

    /** The BST +/- ranges to try in order: the configured value first, then 100, 150, 200, ... up to the cap. */
    private static List<Integer> rangeSequence(int base) {
        List<Integer> ranges = new ArrayList<>();
        ranges.add(Math.max(0, base));
        for (int r = 100; r <= BST_RANGE_CAP; r += 50) {
            if (r > base) {
                ranges.add(r);
            }
        }
        return ranges;
    }

    /** Resolves an ally's spec once, computing BST, evolution stage, and (optionally) weakness. */
    private static AllyInfo resolve(RaidAlly ally, boolean avoidWeakness, List<Holder<Type>> bossTypes) {
        double weight = ally.weight();
        String spec = ally.spec();
        boolean useCache = CCDelicTweaksConfig.raidAllyCacheBst();
        CachedStats cached = useCache ? STATS_CACHE.get(spec) : null;

        // We need the form on a cache miss, or whenever we must evaluate per-raid type weakness.
        Stats form = null;
        if (cached == null || avoidWeakness) {
            try {
                Pokemon temp = ally.specification().create();
                form = temp == null ? null : temp.getForm();
            } catch (Throwable t) {
                CCDelicTweaksMod.LOGGER.warn("[PixelmonAdjustments] Could not parse raid ally spec '{}'; excluding it.", spec);
            }
        }

        int bst;
        boolean firstStage;
        if (cached != null) {
            bst = cached.bst();
            firstStage = cached.firstStage();
        } else if (form != null) {
            bst = safeBst(form);
            firstStage = isFirstStage(form);
            if (useCache && bst > 0) {
                STATS_CACHE.put(spec, new CachedStats(bst, firstStage));
            }
        } else {
            bst = -1;          // Unparseable -> never fits a BST window, so it is filtered out.
            firstStage = false;
        }

        boolean weak = false;
        if (avoidWeakness && form != null) {
            weak = isWeakTo(form.getTypes(), bossTypes);
        }
        return new AllyInfo(ally, weight, bst, weak, firstStage);
    }

    /**
     * @return true if this form is a first-stage evolution: it can still evolve but has no
     *         pre-evolution (e.g. Squirtle, Eevee). Single-stage (can't evolve), middle, and final
     *         forms all return false. Unknown/errored evolution data returns false (not excluded).
     */
    private static boolean isFirstStage(Stats form) {
        try {
            List<?> evolutions = form.getEvolutions();
            if (evolutions == null || evolutions.isEmpty()) {
                return false; // Cannot evolve -> single-stage or fully evolved -> allowed.
            }
            boolean hasPreEvolution;
            try {
                hasPreEvolution = !form.getPreEvolutions().isEmpty();
            } catch (Throwable t) {
                hasPreEvolution = false; // Null pre-evolution list -> no pre-evolution.
            }
            return !hasPreEvolution; // Can evolve and has no pre-evolution -> first stage.
        } catch (Throwable t) {
            return false;
        }
    }

    /** @return the BST of the first human player's locked-in Pokemon, or -1 if none. */
    private static int playerBst(RaidData raid) {
        List<RaidData.RaidPlayer> players = raid.getPlayers();
        if (players == null) {
            return -1;
        }
        for (RaidData.RaidPlayer rp : players) {
            if (rp != null && rp.isPlayer() && rp.pokemon != null) {
                int bst = safeBst(rp.pokemon.getForm());
                if (bst > 0) {
                    return bst;
                }
            }
        }
        return -1;
    }

    /** @return true if any of the boss's types hits the ally's types for >= 2x damage. */
    private static boolean isWeakTo(List<Holder<Type>> allyTypes, List<Holder<Type>> bossTypes) {
        if (allyTypes == null || allyTypes.isEmpty() || bossTypes == null || bossTypes.isEmpty()) {
            return false;
        }
        for (Holder<Type> bossType : bossTypes) {
            if (bossType == null) {
                continue;
            }
            Type type = bossType.value();
            if (type != null && type.getTotalEffectiveness(allyTypes, false) >= 2.0) {
                return true;
            }
        }
        return false;
    }

    /** Sums the six base stats. Returns -1 on any failure so the caller can filter the entry out. */
    private static int safeBst(Stats form) {
        try {
            if (form == null) {
                return -1;
            }
            ImmutableBattleStats stats = form.getBattleStats();
            if (stats == null) {
                return -1;
            }
            int total = 0;
            for (BattleStatsType stat : BattleStatsType.EV_IV_STATS) {
                total += stats.getStat(stat);
            }
            return total;
        } catch (Throwable t) {
            return -1;
        }
    }
}
