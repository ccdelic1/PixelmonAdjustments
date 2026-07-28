package com.ccdelic.tweaks.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.ccdelic.tweaks.CCDelicTweaksMod;
import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.ccdelic.tweaks.legendary.LegendaryRecentTracker;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.spawning.AbstractSpawner;
import com.pixelmonmod.pixelmon.api.spawning.SpawnAction;
import com.pixelmonmod.pixelmon.api.spawning.SpawnInfo;
import com.pixelmonmod.pixelmon.api.spawning.SpawnLocation;
import com.pixelmonmod.pixelmon.api.spawning.SpawnSet;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.algorithms.selection.FlatDistinctAlgorithm;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnActionPokemon;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon;
import com.pixelmonmod.pixelmon.spawning.LegendarySpawner;

import net.minecraft.world.entity.Entity;

/**
 * Legendary Spawn Anti-Repeat (READ path).
 *
 * <p>The natural legendary spawner selects its Pokemon with {@link FlatDistinctAlgorithm}: it builds
 * the pool of species valid for the spawn locations, then picks exactly one (rarity-weighted) and
 * returns a single-element list. Because the choice is made inside the algorithm, we cannot simply
 * filter the returned list -- that would only ever empty it. Instead we <b>wrap the whole method</b>
 * ({@code @WrapMethod}) and use <b>rejection sampling</b>: if the algorithm's pick is a recently
 * spawned legendary and at least one non-recent species is available for these locations, we re-run
 * the (re-rolling) algorithm until a non-recent species is chosen. This preserves Pixelmon's relative
 * rarity weighting among the allowed species.
 *
 * <p>Wrapping the method itself (rather than an {@code INVOKE} inside {@code LegendarySpawner}'s
 * {@code thenApply} lambda) avoids fragile lambda-name targeting -- {@code calculateSpawnActions} is a
 * stable public method. The wrapper only acts for the natural legendary spawner
 * ({@code firesChooseEvent == true}); the mega boss spawner and any other caller pass through
 * untouched. It never throws: any error falls back to the algorithm's original result.
 */
@Mixin(FlatDistinctAlgorithm.class)
public abstract class FlatDistinctAlgorithmMixin {

    @Unique
    private static final int CCDELIC_MAX_RETRIES = 64;

    @WrapMethod(method = "calculateSpawnActions", remap = false)
    private List<SpawnAction<? extends Entity>> ccdelic$antiRepeat(
            AbstractSpawner spawner,
            List<SpawnSet> spawnSets,
            List<SpawnLocation> spawnLocations,
            Operation<List<SpawnAction<? extends Entity>>> original) {

        List<SpawnAction<? extends Entity>> result = original.call(spawner, spawnSets, spawnLocations);
        try {
            // Only the natural legendary spawner -- NOT the mega boss spawner (firesChooseEvent == false).
            if (!(spawner instanceof LegendarySpawner ls) || !ls.firesChooseEvent) {
                return result;
            }
            if (!CCDelicTweaksConfig.antiRepeatEnabled() || CCDelicTweaksConfig.recentCount() <= 0) {
                return result;
            }
            if (result == null || result.isEmpty()) {
                return result;
            }

            String chosen = ccdelic$speciesOf(result);
            boolean log = CCDelicTweaksConfig.logFiltered();
            if (chosen == null || !LegendaryRecentTracker.isRecent(chosen)) {
                if (log) {
                    CCDelicTweaksMod.LOGGER.info(
                            "[PixelmonAdjustments] Anti-repeat: '{}' is not recent {} -- allowing.",
                            chosen, LegendaryRecentTracker.snapshot());
                }
                return result; // fresh (or unidentifiable) -- let it through
            }

            // The chosen legendary is recent. Only re-roll if a non-recent option exists; otherwise
            // excluding it would leave zero candidates, so keep the original.
            if (!ccdelic$hasNonRecentCandidate(spawner, spawnLocations)) {
                if (log) {
                    CCDelicTweaksMod.LOGGER.info(
                            "[PixelmonAdjustments] Anti-repeat: '{}' is recent but is the only candidate for these"
                                    + " locations -- allowing.", chosen);
                }
                return result;
            }

            for (int i = 0; i < CCDELIC_MAX_RETRIES; i++) {
                List<SpawnAction<? extends Entity>> retry = original.call(spawner, spawnSets, spawnLocations);
                if (retry == null || retry.isEmpty()) {
                    continue;
                }
                String candidate = ccdelic$speciesOf(retry);
                if (candidate != null && !LegendaryRecentTracker.isRecent(candidate)) {
                    if (log) {
                        CCDelicTweaksMod.LOGGER.info(
                                "[PixelmonAdjustments] Anti-repeat: replaced recent legendary '{}' with '{}' (recent {}).",
                                chosen, candidate, LegendaryRecentTracker.snapshot());
                    }
                    return retry;
                }
            }

            // Exhausted retries without landing a non-recent species; fall back to the original choice.
            if (log) {
                CCDelicTweaksMod.LOGGER.info(
                        "[PixelmonAdjustments] Anti-repeat: no non-recent legendary after {} tries; allowing '{}'.",
                        CCDELIC_MAX_RETRIES, chosen);
            }
            return result;
        } catch (Throwable t) {
            CCDelicTweaksMod.LOGGER.error("[PixelmonAdjustments] Anti-repeat filter error; using original spawn.", t);
            return result;
        }
    }

    /** Extracts the internal species name from the first Pokemon spawn action, or {@code null}. */
    @Unique
    private static String ccdelic$speciesOf(List<SpawnAction<? extends Entity>> actions) {
        for (SpawnAction<? extends Entity> action : actions) {
            if (action instanceof SpawnActionPokemon sap && sap.pokemon != null) {
                Species species = sap.pokemon.getSpecies();
                if (species != null) {
                    return species.getName();
                }
            }
        }
        return null;
    }

    /**
     * @return {@code true} if any suitable spawn across the given locations is a Pokemon whose
     *         species is NOT currently recent (unidentifiable species count as non-recent so we
     *         never over-filter).
     */
    @Unique
    private static boolean ccdelic$hasNonRecentCandidate(AbstractSpawner spawner, List<SpawnLocation> locations) {
        for (SpawnLocation location : locations) {
            List<SpawnInfo> suitable = spawner.getSuitableSpawns(location);
            if (suitable == null) {
                continue;
            }
            for (SpawnInfo info : suitable) {
                String name = null;
                if (info instanceof SpawnInfoPokemon sip) {
                    // getSpecies(), not the raw 'species' field: that field is only populated as a
                    // side effect of this getter, so reading it directly returns null for any
                    // SpawnInfo nothing has resolved yet -- which made every candidate look
                    // unidentifiable, and so made this whole check always report true.
                    Species species = sip.getSpecies();
                    if (species != null) {
                        name = species.getName();
                    }
                }
                if (name == null || !LegendaryRecentTracker.isRecent(name)) {
                    return true;
                }
            }
        }
        return false;
    }
}
