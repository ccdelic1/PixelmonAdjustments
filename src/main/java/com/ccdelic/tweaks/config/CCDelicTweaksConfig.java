package com.ccdelic.tweaks.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration for Pixelmon: Adjustments.
 *
 * <p>Registered as a {@link net.neoforged.fml.config.ModConfig.Type#COMMON} config, so the file is
 * created at {@code config/PixelmonAdjustments.toml} while the game loads -- not when a world is
 * created -- and one set of settings applies to every world. All values are read live through the
 * accessor methods, so editing the file takes effect without a restart for the features that
 * re-evaluate as they run.
 *
 * <p>Every feature here is server-side logic; COMMON simply means "loaded on both physical sides and
 * not tied to a save", which is what makes the file appear at startup.
 *
 * <h2>Comment style</h2>
 * The generated file is meant to stay readable, so follow these when adding an option:
 * <ul>
 *   <li>Every option comment starts with an empty {@code ""} line. NightConfig writes each comment
 *       line as {@code <indent>#<text>}, so an empty one renders as a bare {@code #} that separates
 *       the option from the one above it. It cannot render as a truly blank line -- the writer always
 *       emits the {@code #} -- and this is the closest available substitute.</li>
 *   <li>Keep the rest of the comment to a single line.</li>
 *   <li>A section's master toggle gets no comment at all; the section header already explains it, and
 *       it sits directly under that header so it needs no separator.</li>
 *   <li>Do not hand-write {@code Default:} or {@code Range:} lines. {@code defineInRange} appends both
 *       automatically, on two separate lines, and that is not configurable.</li>
 *   <li>Never let a comment be entirely blank -- NeoForge throws on an all-whitespace comment.</li>
 * </ul>
 */
public final class CCDelicTweaksConfig {

    public static final ModConfigSpec SPEC;

    // --- legendary_anti_repeat ---
    private static final ModConfigSpec.BooleanValue ANTI_REPEAT_ENABLED;
    private static final ModConfigSpec.IntValue ANTI_REPEAT_RECENT_COUNT;
    private static final ModConfigSpec.BooleanValue ANTI_REPEAT_LOG_FILTERED;

    // --- legendary_coords_broadcast ---
    private static final ModConfigSpec.BooleanValue LEGENDARY_COORDS_ENABLED;

    // --- ultra_beast_alert ---
    private static final ModConfigSpec.BooleanValue UB_ALERT_ENABLED;

    // --- starter_kit ---
    private static final ModConfigSpec.BooleanValue STARTER_KIT_ENABLED;

    // --- wooden_button_recipes ---
    private static final ModConfigSpec.BooleanValue WOODEN_BUTTONS_FOR_IRON_BASES;

    // --- village_npc_coexistence ---
    private static final ModConfigSpec.BooleanValue SPAWN_VANILLA_VILLAGERS;

    // --- machine_parts ---
    private static final ModConfigSpec.BooleanValue MACHINE_PARTS_ENABLED;

    // --- early_stage_level_unlock ---
    private static final ModConfigSpec.BooleanValue EARLY_STAGE_ENABLED;
    private static final ModConfigSpec.IntValue EARLY_STAGE_MIN_LEVEL;
    private static final ModConfigSpec.BooleanValue EARLY_STAGE_EXCLUDE_RARE;
    private static final ModConfigSpec.BooleanValue EARLY_STAGE_LOG_ADJUSTMENTS;

    // --- early_stage_time_unlock ---
    private static final ModConfigSpec.BooleanValue TIME_UNLOCK_ENABLED;
    private static final ModConfigSpec.BooleanValue TIME_UNLOCK_EXCLUDE_RARE;
    private static final ModConfigSpec.BooleanValue TIME_UNLOCK_UPDATE_WIKI;
    private static final ModConfigSpec.BooleanValue TIME_UNLOCK_LOG_ADJUSTMENTS;

    // --- raid_ally_filter ---
    private static final ModConfigSpec.BooleanValue RAID_ALLY_ENABLED;
    private static final ModConfigSpec.IntValue RAID_ALLY_BST_RANGE;
    private static final ModConfigSpec.BooleanValue RAID_ALLY_AVOID_WEAKNESS;
    private static final ModConfigSpec.BooleanValue RAID_ALLY_EXCLUDE_FIRST_STAGE;
    private static final ModConfigSpec.BooleanValue RAID_ALLY_LOG_FILTERED;
    private static final ModConfigSpec.BooleanValue RAID_ALLY_CACHE_BST;

    // --- loot_override ---
    private static final ModConfigSpec.BooleanValue LOOT_ENABLED;
    private static final ModConfigSpec.IntValue LOOT_MIN_ROLLS;
    private static final ModConfigSpec.IntValue LOOT_MAX_ROLLS;
    private static final ModConfigSpec.IntValue LOOT_COMMON_WEIGHT;
    private static final ModConfigSpec.IntValue LOOT_UNCOMMON_WEIGHT;
    private static final ModConfigSpec.IntValue LOOT_RARE_WEIGHT;
    private static final ModConfigSpec.IntValue LOOT_EPIC_WEIGHT;
    private static final ModConfigSpec.IntValue LOOT_LEGENDARY_WEIGHT;

    // --- shop_override ---
    private static final ModConfigSpec.BooleanValue SHOP_ENABLED;
    private static final ModConfigSpec.BooleanValue SHOP_ONLY_POKEMARTS;
    private static final ModConfigSpec.BooleanValue SHOP_REMOVE_OTHER_BALLS;
    private static final ModConfigSpec.DoubleValue SHOP_POKEBALL_BUY_PRICE;
    private static final ModConfigSpec.DoubleValue SHOP_POKEBALL_SELL_PRICE;
    private static final ModConfigSpec.BooleanValue SHOP_ADD_REVIVE;
    private static final ModConfigSpec.DoubleValue SHOP_REVIVE_BUY_PRICE;
    private static final ModConfigSpec.DoubleValue SHOP_REVIVE_SELL_PRICE;
    private static final ModConfigSpec.BooleanValue SHOP_LOG_OVERRIDES;

    /** Upper bound for shop prices. Far beyond any practical value, but small enough that the
     *  generated {@code Range:} line stays readable instead of printing Double.MAX_VALUE. */
    private static final double MAX_PRICE = 1_000_000.0;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Prevents the same legendary Pokemon from spawning back-to-back in natural legendary spawns.")
                .push("legendary_anti_repeat");

        ANTI_REPEAT_ENABLED = builder
                .define("enabled", true);

        ANTI_REPEAT_RECENT_COUNT = builder
                .comment("", "How many of the most recent successful legendary spawns to remember and exclude. Set to 0 to effectively disable filtering.")
                .defineInRange("recent_count", 3, 0, 10);

        ANTI_REPEAT_LOG_FILTERED = builder
                .comment("", "Log to the console whenever a legendary is filtered out / replaced by this feature (debug aid).")
                .define("log_filtered_spawns", false);

        builder.pop();

        builder.comment("Announces the X/Y/Z coordinates of a legendary spawn to all players in chat.")
                .push("legendary_coords_broadcast");

        LEGENDARY_COORDS_ENABLED = builder
                .define("doLegendaryCoordinates", true);

        builder.pop();

        builder.comment("Alerts the single nearest player (only) when an Ultra Beast spawns nearby.")
                .push("ultra_beast_alert");

        UB_ALERT_ENABLED = builder
                .define("doUltraBeastNotification", true);

        builder.pop();

        builder.comment("Grants a one-time starter kit the very first time a player ever joins the server.")
                .push("starter_kit");

        STARTER_KIT_ENABLED = builder
                .define("doStarterKit", true);

        builder.pop();

        builder.comment("Adds a second crafting recipe variant for every Poke Ball that accepts an iron base letting them also be crafted with a wooden button in place of the stone button.")
                .push("wooden_button_recipes");

        WOODEN_BUTTONS_FOR_IRON_BASES = builder
                .define("doWoodenButtonsForIronBases", true);

        builder.pop();

        builder.comment("Allow vanilla villager spawns ALONGSIDE the Pixelmon NPC that takes its place.")
                .push("village_npc_coexistence");

        SPAWN_VANILLA_VILLAGERS = builder
                .define("doSpawnVanillaVillagers", true);

        builder.pop();

        builder.comment("Healer/PC blocks drop a custom part instead of an aluminum ingot when broken without Silk Touch.")
                .push("machine_parts");

        MACHINE_PARTS_ENABLED = builder
                .define("enabled", true);

        builder.pop();

        builder.comment("Removes the spawn LEVEL FLOOR for Pokemon that are at the start of their evolution line or are single-stage Pokemon.")
                .push("early_stage_level_unlock");

        EARLY_STAGE_ENABLED = builder
                .define("enabled", false);

        EARLY_STAGE_MIN_LEVEL = builder
                .comment("", "The level floor to apply to eligible spawns. 1 means 'any level from 1 up to the set's own maximum'.")
                .defineInRange("min_level", 3, 1, 100);

        EARLY_STAGE_EXCLUDE_RARE = builder
                .comment("", "Whether to exclude Legendaries, Mythicals, Ultra Beasts and Paradox Pokemon from this rule.")
                .define("exclude_rare_species", true);

        EARLY_STAGE_LOG_ADJUSTMENTS = builder
                .comment("", "Log to the console every time a spawn's level floor is lowered (debug aid; very spammy).")
                .define("log_adjustments", false);

        builder.pop();

        builder.comment("Removes the TIME OF DAY spawn gate for Pokemon that are at the start of their evolution line or are single-stage Pokemon.")
                .push("early_stage_time_unlock");

        TIME_UNLOCK_ENABLED = builder
                .define("enabled", false);

        TIME_UNLOCK_EXCLUDE_RARE = builder
                .comment("", "Whether to exclude Legendaries, Mythicals, Ultra Beasts and Paradox Pokemon from this rule.")
                .define("exclude_rare_species", true);

        TIME_UNLOCK_UPDATE_WIKI = builder
                .comment("", "Hide the 'Time:' line in /wiki <pokemon> spawning for Pokemon whose time gate this feature removes. Only functions if the time of day spawn gate feature is enabled.")
                .define("update_wiki_display", true);

        TIME_UNLOCK_LOG_ADJUSTMENTS = builder
                .comment("", "Log to the console every time a time-of-day gate is bypassed (debug aid; extremely spammy)")
                .define("log_adjustments", false);

        builder.pop();

        builder.comment("Smarter AI ally selection for solo Raid Dens: match ally power to the fight and avoid type-disadvantaged allies.")
                .push("raid_ally_filter");

        RAID_ALLY_ENABLED = builder
                .define("enabled", true);

        RAID_ALLY_BST_RANGE = builder
                .comment("", "How far ABOVE the stronger of the player/boss BST an ally may be.")
                .defineInRange("bst_range", 30, 0, 200);

        RAID_ALLY_AVOID_WEAKNESS = builder
                .comment("", "If true, exclude allies whose type(s) are super-effectively hit by the raid boss's type(s), unless doing so would leave no eligible allies.")
                .define("avoid_type_weakness", true);

        RAID_ALLY_EXCLUDE_FIRST_STAGE = builder
                .comment("", "If true, exclude first-stage (unevolved-but-evolvable) Pokemon as raid allies, so partners are only single-stage, middle, or fully-evolved Pokemon. Falls back if it would leave none.")
                .define("exclude_first_stage", true);

        RAID_ALLY_LOG_FILTERED = builder
                .comment("", "Log removed allies to the console for debugging.")
                .define("log_filtered_allies", false);

        RAID_ALLY_CACHE_BST = builder
                .comment("", "Cache computed BST values per ally spec to avoid repeated Pokemon spec parsing.")
                .define("cache_bst", true);

        builder.pop();

        builder.comment("Replaces the loot in Pixelmon Arena Stalls (chests/arena) and PokeMart (chests/waypoint) chests with a curated, tiered pool.")
                .push("loot_override");

        LOOT_ENABLED = builder
                .define("enabled", true);

        LOOT_MIN_ROLLS = builder
                .comment("", "Minimum number of items per chest.")
                .defineInRange("min_rolls", 1, 1, 64);

        LOOT_MAX_ROLLS = builder
                .comment("", "Maximum number of items per chest.")
                .defineInRange("max_rolls", 6, 1, 64);

        LOOT_COMMON_WEIGHT = builder
                .comment("", " Relative weight of each WHOLE rarity tier (NOT per-item).",
                        "",
                        " A tier's weight is split evenly across all of its items, so a tier's per-roll chance depends only on its weight and not on how many items it contains.",
                        " A tier's chance = its weight / the sum of all tier weights, with the defaults that is roughly Common 84%, Uncommon 13%, Rare 2%, Epic 0.7%, Legendary 0.03%.",
                        "",
                        " Weight of the whole Common tier: (Poke Ball, Great Ball, Potion, Revive, and the Iron/Aluminum/Silver Ingots)")
                .defineInRange("common_weight", 5000, 0, 100000);

        LOOT_UNCOMMON_WEIGHT = builder
                .comment("", "Weight of the whole Uncommon tier: (Ultra Ball, Quick Ball, Super Potion, Full Heal, Elixir, Platinum Ingot, and drinks)")
                .defineInRange("uncommon_weight", 800, 0, 100000);

        LOOT_RARE_WEIGHT = builder
                .comment("", "Weight of the whole Rare tier (Hyper Potion, Max Revive/Elixir, Soothe Bell, ...).")
                .defineInRange("rare_weight", 120, 0, 100000);

        LOOT_EPIC_WEIGHT = builder
                .comment("", "Weight of the whole Epic tier (Max Potion, Full Restore, power items, ability items, Poke Bags, ...).")
                .defineInRange("epic_weight", 40, 0, 100000);

        LOOT_LEGENDARY_WEIGHT = builder
                .comment("", "Weight of the whole Legendary tier: (Evolution Stones, Lures, and the Orb)")
                .defineInRange("legendary_weight", 2, 0, 100000);

        builder.pop();

        builder.comment("Overrides NPC PokeMart shopkeeper inventories: sell only the standard Poke Ball, plus Revives.")
                .push("shop_override");

        SHOP_ENABLED = builder
                .define("enabled", true);

        SHOP_ONLY_POKEMARTS = builder
                .comment("", "Only modify shops that sell at least one Poke Ball (PokeMarts).")
                .define("only_pokemarts", true);

        SHOP_REMOVE_OTHER_BALLS = builder
                .comment("", "Remove every non-standard Poke Ball type (Great Ball, Ultra Ball, Luxury Ball, ...) from the shop.")
                .define("remove_other_balls", true);

        SHOP_POKEBALL_BUY_PRICE = builder
                .comment("", "Buy price for a standard Poke Ball.")
                .defineInRange("pokeball_buy_price", 500.0, 0.0, MAX_PRICE);

        SHOP_POKEBALL_SELL_PRICE = builder
                .comment("", "Sell-back price for a standard Poke Ball. Keep below the buy price to avoid buy/sell exploits.")
                .defineInRange("pokeball_sell_price", 165.0, 0.0, MAX_PRICE);

        SHOP_ADD_REVIVE = builder
                .comment("", "Ensure a Revive is on sale at the configured price (updates an existing entry, or adds one).")
                .define("add_revive", true);

        SHOP_REVIVE_BUY_PRICE = builder
                .comment("", "Buy price for a Revive.")
                .defineInRange("revive_buy_price", 500.0, 0.0, MAX_PRICE);

        SHOP_REVIVE_SELL_PRICE = builder
                .comment("", "Sell-back price for a Revive. Keep below the buy price to avoid buy/sell exploits.")
                .defineInRange("revive_sell_price", 165.0, 0.0, MAX_PRICE);

        SHOP_LOG_OVERRIDES = builder
                .comment("", "Log to the console whenever a shop's inventory is overridden (debug aid; can be spammy).")
                .define("log_overrides", false);

        builder.pop();

        SPEC = builder.build();
    }

    private CCDelicTweaksConfig() {
    }

    // Config values throw if read before the config is loaded. These helpers fall back to the
    // declared default in that window (e.g. a loot table loading very early during startup), so a
    // feature never crashes on an unloaded-config read.
    private static boolean safe(ModConfigSpec.BooleanValue value, boolean fallback) {
        return SPEC.isLoaded() ? value.get() : fallback;
    }

    private static int safe(ModConfigSpec.IntValue value, int fallback) {
        return SPEC.isLoaded() ? value.get() : fallback;
    }

    private static double safe(ModConfigSpec.DoubleValue value, double fallback) {
        return SPEC.isLoaded() ? value.get() : fallback;
    }

    // --- legendary_anti_repeat accessors ---

    /** @return whether the legendary anti-repeat feature is enabled. */
    public static boolean antiRepeatEnabled() {
        return safe(ANTI_REPEAT_ENABLED, true);
    }

    /** @return how many recent legendary spawns to remember and exclude (0-10). */
    public static int recentCount() {
        return safe(ANTI_REPEAT_RECENT_COUNT, 3);
    }

    /** @return whether filtered/replaced legendary spawns should be logged. */
    public static boolean logFiltered() {
        return safe(ANTI_REPEAT_LOG_FILTERED, false);
    }

    // --- legendary_coords_broadcast accessors ---

    /** @return whether legendary spawn coordinates should be broadcast to all players. */
    public static boolean doLegendaryCoordinates() {
        return safe(LEGENDARY_COORDS_ENABLED, true);
    }

    // --- ultra_beast_alert accessors ---

    /** @return whether the nearest-player Ultra Beast spawn alert is enabled. */
    public static boolean doUltraBeastNotification() {
        return safe(UB_ALERT_ENABLED, true);
    }

    // --- starter_kit accessors ---

    /** @return whether the one-time, first-join starter kit is enabled. */
    public static boolean doStarterKit() {
        return safe(STARTER_KIT_ENABLED, true);
    }

    // --- wooden_button_recipes accessors ---

    /** @return whether the iron-base wooden-button Poke Ball recipe variants are enabled. */
    public static boolean doWoodenButtonsForIronBases() {
        return safe(WOODEN_BUTTONS_FOR_IRON_BASES, true);
    }

    // --- village_npc_coexistence accessors ---

    /** @return whether vanilla Villagers should be left alone to spawn alongside Pixelmon's NPCs. */
    public static boolean doSpawnVanillaVillagers() {
        return safe(SPAWN_VANILLA_VILLAGERS, true);
    }

    // --- machine_parts accessors ---

    /** @return whether Healer/PC blocks should drop custom parts instead of aluminum ingots. */
    public static boolean machinePartsEnabled() {
        return safe(MACHINE_PARTS_ENABLED, true);
    }

    // --- early_stage_level_unlock accessors ---

    /** @return whether the spawn level floor should be removed for single-stage/first-stage Pokemon. */
    public static boolean earlyStageLevelUnlockEnabled() {
        return safe(EARLY_STAGE_ENABLED, false);
    }

    /** @return the level floor to apply to eligible spawns (1 = no floor). */
    public static int earlyStageMinLevel() {
        return safe(EARLY_STAGE_MIN_LEVEL, 3);
    }

    /** @return whether Legendaries/Mythicals/Ultra Beasts/Paradox Pokemon keep their original level ranges. */
    public static boolean earlyStageExcludeRareSpecies() {
        return safe(EARLY_STAGE_EXCLUDE_RARE, true);
    }

    /** @return whether every lowered spawn level floor should be logged. */
    public static boolean earlyStageLogAdjustments() {
        return safe(EARLY_STAGE_LOG_ADJUSTMENTS, false);
    }

    // --- early_stage_time_unlock accessors ---

    /** @return whether the time-of-day spawn gate should be removed for single-stage/first-stage Pokemon. */
    public static boolean earlyStageTimeUnlockEnabled() {
        return safe(TIME_UNLOCK_ENABLED, false);
    }

    /** @return whether Legendaries/Mythicals/Ultra Beasts/Paradox Pokemon keep their time restrictions. */
    public static boolean timeUnlockExcludeRareSpecies() {
        return safe(TIME_UNLOCK_EXCLUDE_RARE, true);
    }

    /** @return whether {@code /wiki} should stop showing a time restriction this feature no longer enforces. */
    public static boolean timeUnlockUpdateWikiDisplay() {
        return safe(TIME_UNLOCK_UPDATE_WIKI, true);
    }

    /** @return whether every bypassed time-of-day gate should be logged. */
    public static boolean timeUnlockLogAdjustments() {
        return safe(TIME_UNLOCK_LOG_ADJUSTMENTS, false);
    }

    // --- raid_ally_filter accessors ---

    /** @return whether smart raid ally selection is enabled. */
    public static boolean raidAllyEnabled() {
        return safe(RAID_ALLY_ENABLED, true);
    }

    /** @return the +/- BST range applied around the player/boss BST window. */
    public static int raidAllyBstRange() {
        return safe(RAID_ALLY_BST_RANGE, 30);
    }

    /** @return whether type-disadvantaged allies should be excluded (with fallback). */
    public static boolean raidAllyAvoidWeakness() {
        return safe(RAID_ALLY_AVOID_WEAKNESS, true);
    }

    /** @return whether first-stage (unevolved-but-evolvable) Pokemon should be excluded as allies (with fallback). */
    public static boolean raidAllyExcludeFirstStage() {
        return safe(RAID_ALLY_EXCLUDE_FIRST_STAGE, true);
    }

    /** @return whether removed allies should be logged. */
    public static boolean raidAllyLogFiltered() {
        return safe(RAID_ALLY_LOG_FILTERED, false);
    }

    /** @return whether computed ally BSTs should be cached per spec. */
    public static boolean raidAllyCacheBst() {
        return safe(RAID_ALLY_CACHE_BST, true);
    }

    // --- loot_override accessors ---

    /** @return whether the village chest loot override is enabled. */
    public static boolean lootOverrideEnabled() {
        return safe(LOOT_ENABLED, true);
    }

    /** @return minimum item rolls per overridden chest. */
    public static int lootMinRolls() {
        return safe(LOOT_MIN_ROLLS, 1);
    }

    /** @return maximum item rolls per overridden chest. */
    public static int lootMaxRolls() {
        return safe(LOOT_MAX_ROLLS, 6);
    }

    /** @return weight of the whole Common (dominant) tier. */
    public static int lootCommonWeight() {
        return safe(LOOT_COMMON_WEIGHT, 5000);
    }

    /** @return weight of the whole Uncommon tier. */
    public static int lootUncommonWeight() {
        return safe(LOOT_UNCOMMON_WEIGHT, 800);
    }

    /** @return weight of the whole Rare tier. */
    public static int lootRareWeight() {
        return safe(LOOT_RARE_WEIGHT, 120);
    }

    /** @return weight of the whole Epic tier (also the combined weight of all colored Poke Bags). */
    public static int lootEpicWeight() {
        return safe(LOOT_EPIC_WEIGHT, 40);
    }

    /** @return weight of the whole Legendary (ultra-rare: Evolution Stones/Lures/Orb) tier. */
    public static int lootLegendaryWeight() {
        return safe(LOOT_LEGENDARY_WEIGHT, 2);
    }

    // --- shop_override accessors ---

    /** @return whether the shop inventory override feature is enabled. */
    public static boolean shopOverrideEnabled() {
        return safe(SHOP_ENABLED, true);
    }

    /** @return whether only PokeMart shops (those selling a Poke Ball) should be modified. */
    public static boolean shopOnlyPokemarts() {
        return safe(SHOP_ONLY_POKEMARTS, true);
    }

    /** @return whether non-standard Poke Ball types should be removed from shops. */
    public static boolean shopRemoveOtherBalls() {
        return safe(SHOP_REMOVE_OTHER_BALLS, true);
    }

    /** @return the buy price to apply to the standard Poke Ball. */
    public static double shopPokeballBuyPrice() {
        return safe(SHOP_POKEBALL_BUY_PRICE, 500.0);
    }

    /** @return the sell-back price to apply to the standard Poke Ball. */
    public static double shopPokeballSellPrice() {
        return safe(SHOP_POKEBALL_SELL_PRICE, 165.0);
    }

    /** @return whether a Revive should be ensured on sale at the configured price. */
    public static boolean shopAddRevive() {
        return safe(SHOP_ADD_REVIVE, true);
    }

    /** @return the buy price to apply to Revives. */
    public static double shopReviveBuyPrice() {
        return safe(SHOP_REVIVE_BUY_PRICE, 500.0);
    }

    /** @return the sell-back price to apply to Revives. */
    public static double shopReviveSellPrice() {
        return safe(SHOP_REVIVE_SELL_PRICE, 165.0);
    }

    /** @return whether shop overrides should be logged. */
    public static boolean shopLogOverrides() {
        return safe(SHOP_LOG_OVERRIDES, false);
    }
}
