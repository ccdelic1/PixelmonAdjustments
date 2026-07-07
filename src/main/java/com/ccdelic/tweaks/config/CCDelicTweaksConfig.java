package com.ccdelic.tweaks.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side configuration for CCDelic Tweaks.
 *
 * <p>Registered as a {@link net.neoforged.fml.config.ModConfig.Type#SERVER} config so the
 * settings live per-world in {@code config/ccdelictweaks-server.toml}, are authoritative on the
 * server, and reload automatically when the world (re)loads. All values are read live via the
 * accessor methods so that {@code /reload} takes effect without a restart.
 */
public final class CCDelicTweaksConfig {

    public static final ModConfigSpec SPEC;

    // --- legendary_anti_repeat ---
    private static final ModConfigSpec.BooleanValue ANTI_REPEAT_ENABLED;
    private static final ModConfigSpec.IntValue ANTI_REPEAT_RECENT_COUNT;
    private static final ModConfigSpec.BooleanValue ANTI_REPEAT_PERSIST_HISTORY;
    private static final ModConfigSpec.BooleanValue ANTI_REPEAT_LOG_FILTERED;

    // --- machine_parts ---
    private static final ModConfigSpec.BooleanValue MACHINE_PARTS_ENABLED;

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

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Prevents the same legendary Pokemon from spawning back-to-back in natural legendary spawns.")
                .push("legendary_anti_repeat");

        ANTI_REPEAT_ENABLED = builder
                .comment("Master toggle for the legendary anti-repeat feature.")
                .define("enabled", true);

        ANTI_REPEAT_RECENT_COUNT = builder
                .comment("How many of the most recent successful legendary spawns to remember and exclude.",
                        "A legendary that spawned within this many spawns will not spawn again -- unless it is the",
                        "only legendary the current biome/conditions allow. Set to 0 to effectively disable filtering.")
                .defineInRange("recent_count", 3, 0, 10);

        ANTI_REPEAT_PERSIST_HISTORY = builder
                .comment("Whether to persist the recent legendary history across server restarts.",
                        "NOTE: not yet implemented; history is currently in-memory only and resets on restart.")
                .define("persist_history", false);

        ANTI_REPEAT_LOG_FILTERED = builder
                .comment("Log to the console whenever a legendary is filtered out / replaced by this feature (debug aid).")
                .define("log_filtered_spawns", false);

        builder.pop();

        builder.comment("Healer/PC blocks drop a custom part instead of an aluminum ingot when broken without Silk Touch.",
                        "Silk Touch still drops the block itself. Does not affect the (separate) crafting recipes.")
                .push("machine_parts");

        MACHINE_PARTS_ENABLED = builder
                .comment("Master toggle for custom Healer/PC break drops. When false, they drop the default aluminum ingot.")
                .define("enabled", true);

        builder.pop();

        builder.comment("Smarter AI ally selection for solo Raid Dens: match ally power to the fight and avoid type-disadvantaged allies.")
                .push("raid_ally_filter");

        RAID_ALLY_ENABLED = builder
                .comment("Master toggle for smart raid ally selection.")
                .define("enabled", true);

        RAID_ALLY_BST_RANGE = builder
                .comment("How far ABOVE the stronger of the player/boss BST an ally may be. The lower bound is a",
                        "hard floor at the weaker of the two BSTs (allies are never weaker than that).",
                        "With value 30, player BST=550 and raid BST=350, allies must have BST in [350, 580].",
                        "If no ally fits, this widens to 100, then +50 each step (150, 200, ...) -- raising only the",
                        "upper bound -- until one does.")
                .defineInRange("bst_range", 30, 0, 200);

        RAID_ALLY_AVOID_WEAKNESS = builder
                .comment("If true, exclude allies whose type(s) are super-effectively hit by the raid boss's type(s),",
                        "unless doing so would leave no eligible allies.")
                .define("avoid_type_weakness", true);

        RAID_ALLY_EXCLUDE_FIRST_STAGE = builder
                .comment("If true, exclude first-stage (unevolved-but-evolvable) Pokemon as raid allies, so partners",
                        "are only single-stage, middle, or fully-evolved Pokemon. Falls back if it would leave none.")
                .define("exclude_first_stage", true);

        RAID_ALLY_LOG_FILTERED = builder
                .comment("Log removed allies to the console for debugging.")
                .define("log_filtered_allies", false);

        RAID_ALLY_CACHE_BST = builder
                .comment("Cache computed BST values per ally spec to avoid repeated Pokemon spec parsing.")
                .define("cache_bst", true);

        builder.pop();

        builder.comment("Replaces the loot in Pixelmon Arena Stalls (chests/arena) and PokeMart (chests/waypoint) chests",
                        "with a curated, tiered pool. When disabled, the vanilla Pixelmon loot tables are used unchanged.")
                .push("loot_override");

        LOOT_ENABLED = builder
                .comment("Master toggle. When false, the vanilla Pixelmon loot tables for these chests are used.")
                .define("enabled", true);

        LOOT_MIN_ROLLS = builder
                .comment("Minimum number of item rolls per chest.")
                .defineInRange("min_rolls", 1, 1, 64);

        LOOT_MAX_ROLLS = builder
                .comment("Maximum number of item rolls per chest.")
                .defineInRange("max_rolls", 6, 1, 64);

        builder.comment("Relative weight of each WHOLE rarity tier (NOT per-item). A tier's weight is split",
                "evenly across all of its items, so a tier's per-roll chance depends only on its weight and",
                "not on how many items it contains. A tier's chance = its weight / the sum of all tier weights;",
                "with the defaults that is roughly Common 39%, Uncommon 30%, Rare 21%, Epic 9%, Legendary 1.3%.");

        LOOT_COMMON_WEIGHT = builder
                .comment("Weight of the whole Common tier (Potion, Poke Ball, Aluminum Ingot).")
                .defineInRange("common_weight", 450, 0, 100000);
        LOOT_UNCOMMON_WEIGHT = builder
                .comment("Weight of the whole Uncommon tier (Super Potion, Great Ball, Iron/Platinum Ingot).")
                .defineInRange("uncommon_weight", 350, 0, 100000);
        LOOT_RARE_WEIGHT = builder
                .comment("Weight of the whole Rare tier (Hyper Potion, evolution stones, Ultra Ball, lures, ...).")
                .defineInRange("rare_weight", 250, 0, 100000);
        LOOT_EPIC_WEIGHT = builder
                .comment("Weight of the whole Epic tier (Max Potion, power items, ability items, Diamond, ...).",
                        "The 16 colored Poke Bags together count as one item-slot of this tier.")
                .defineInRange("epic_weight", 100, 0, 100000);
        LOOT_LEGENDARY_WEIGHT = builder
                .comment("Weight of the whole Legendary tier (Full Restore, Orb).")
                .defineInRange("legendary_weight", 15, 0, 100000);

        builder.pop();

        builder.comment("Overrides NPC PokeMart shopkeeper inventories: sell only the standard Poke Ball, plus Revives.")
                .push("shop_override");

        SHOP_ENABLED = builder
                .comment("Master toggle for shop inventory overrides. When false, shops use Pixelmon defaults.")
                .define("enabled", true);

        SHOP_ONLY_POKEMARTS = builder
                .comment("Only modify shops that sell at least one Poke Ball (PokeMarts).",
                        "This is how PokeMarts are detected -- it keeps daycare shops, move tutors, etc. untouched.",
                        "Set to false to apply the overrides to ALL NPC shops opened via the shop interaction.")
                .define("only_pokemarts", true);

        SHOP_REMOVE_OTHER_BALLS = builder
                .comment("Remove every non-standard Poke Ball type (Great Ball, Ultra Ball, Luxury Ball, ...) from the shop.")
                .define("remove_other_balls", true);

        SHOP_POKEBALL_BUY_PRICE = builder
                .comment("Buy price for a standard Poke Ball.")
                .defineInRange("pokeball_buy_price", 500.0, 0.0, Double.MAX_VALUE);

        SHOP_POKEBALL_SELL_PRICE = builder
                .comment("Sell-back price for a standard Poke Ball. Keep below the buy price to avoid buy/sell exploits.")
                .defineInRange("pokeball_sell_price", 165.0, 0.0, Double.MAX_VALUE);

        SHOP_ADD_REVIVE = builder
                .comment("Ensure a Revive is on sale at the configured price (updates an existing entry, or adds one).")
                .define("add_revive", true);

        SHOP_REVIVE_BUY_PRICE = builder
                .comment("Buy price for a Revive.")
                .defineInRange("revive_buy_price", 500.0, 0.0, Double.MAX_VALUE);

        SHOP_REVIVE_SELL_PRICE = builder
                .comment("Sell-back price for a Revive. Keep below the buy price to avoid buy/sell exploits.")
                .defineInRange("revive_sell_price", 165.0, 0.0, Double.MAX_VALUE);

        SHOP_LOG_OVERRIDES = builder
                .comment("Log to the console whenever a shop's inventory is overridden (debug aid; can be spammy).")
                .define("log_overrides", false);

        builder.pop();

        SPEC = builder.build();
    }

    private CCDelicTweaksConfig() {
    }

    // Config values throw if read before the server config is loaded. These helpers fall back to the
    // declared default in that window (e.g. a loot table loading very early during server startup),
    // so a feature never crashes on an unloaded-config read.
    private static boolean safe(ModConfigSpec.BooleanValue value, boolean fallback) {
        return SPEC.isLoaded() ? value.get() : fallback;
    }

    private static int safe(ModConfigSpec.IntValue value, int fallback) {
        return SPEC.isLoaded() ? value.get() : fallback;
    }

    private static double safe(ModConfigSpec.DoubleValue value, double fallback) {
        return SPEC.isLoaded() ? value.get() : fallback;
    }

    /** @return whether the legendary anti-repeat feature is enabled. */
    public static boolean antiRepeatEnabled() {
        return safe(ANTI_REPEAT_ENABLED, true);
    }

    /** @return how many recent legendary spawns to remember and exclude (0-10). */
    public static int recentCount() {
        return safe(ANTI_REPEAT_RECENT_COUNT, 3);
    }

    /** @return whether recent-legendary history should persist across restarts (not yet implemented). */
    public static boolean persistHistory() {
        return safe(ANTI_REPEAT_PERSIST_HISTORY, false);
    }

    /** @return whether filtered/replaced legendary spawns should be logged. */
    public static boolean logFiltered() {
        return safe(ANTI_REPEAT_LOG_FILTERED, false);
    }

    // --- machine_parts accessors ---

    /** @return whether Healer/PC blocks should drop custom parts instead of aluminum ingots. */
    public static boolean machinePartsEnabled() {
        return safe(MACHINE_PARTS_ENABLED, true);
    }

    // --- raid_ally_filter accessors ---

    /** @return whether smart raid ally selection is enabled. */
    public static boolean raidAllyEnabled() {
        return safe(RAID_ALLY_ENABLED, true);
    }

    /** @return the +/- BST range applied around the player/boss BST window. */
    public static int raidAllyBstRange() {
        return safe(RAID_ALLY_BST_RANGE, 50);
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

    /** @return weight of each Common-tier item. */
    public static int lootCommonWeight() {
        return safe(LOOT_COMMON_WEIGHT, 450);
    }

    /** @return weight of each Uncommon-tier item. */
    public static int lootUncommonWeight() {
        return safe(LOOT_UNCOMMON_WEIGHT, 350);
    }

    /** @return weight of each Rare-tier item. */
    public static int lootRareWeight() {
        return safe(LOOT_RARE_WEIGHT, 250);
    }

    /** @return weight of each Epic-tier item (also the combined weight of all colored Poke Bags). */
    public static int lootEpicWeight() {
        return safe(LOOT_EPIC_WEIGHT, 100);
    }

    /** @return weight of each Legendary-tier item. */
    public static int lootLegendaryWeight() {
        return safe(LOOT_LEGENDARY_WEIGHT, 15);
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
