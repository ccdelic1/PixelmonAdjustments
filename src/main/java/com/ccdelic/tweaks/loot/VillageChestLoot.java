package com.ccdelic.tweaks.loot;

import java.util.List;
import java.util.Set;

import com.ccdelic.tweaks.CCDelicTweaksMod;
import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.pixelmonmod.api.registry.RegistryValue;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
import com.pixelmonmod.pixelmon.init.registry.ItemRegistration;
import com.pixelmonmod.pixelmon.init.registry.PixelmonDataComponents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/**
 * Custom Loot Tables for Village Chests (Feature #3).
 *
 * <p>Replaces the loot for the two Pixelmon village structure chests -- Arena Stalls
 * ({@code pixelmon:chests/arena}) and PokeMart ({@code pixelmon:chests/waypoint}) -- with a curated,
 * five-tier pool built here in code and swapped in via {@link LootTableLoadEvent}.
 *
 * <p>Building the table programmatically (rather than shipping a datapack override) is what lets the
 * config actually work: when {@code loot_override.enabled} is false we simply leave Pixelmon's table
 * untouched, and the roll count / tier weights are read live from config on every (re)load. All
 * values are read at build time, so {@code /reload} applies changes.
 *
 * <p>Registered on {@code NeoForge.EVENT_BUS} (the event is fired server-side there).
 */
public class VillageChestLoot {

    /** Multiplier applied to a tier's weight before splitting it across items, so per-item weights
     *  in large tiers (e.g. the 34-item Rare tier) don't round down toward zero. */
    private static final int WEIGHT_SCALE = 1000;

    /** The Pixelmon chest loot tables we replace. */
    private static final Set<ResourceLocation> TARGETS = Set.of(
            ResourceLocation.fromNamespaceAndPath("pixelmon", "chests/arena"),
            ResourceLocation.fromNamespaceAndPath("pixelmon", "chests/waypoint"));

    // Item lists per tier (Poke Balls and colored bags are handled separately).
    private static final String[] COMMON_ITEMS = {
            "pixelmon:potion",
            "pixelmon:aluminum_ingot",
            "pixelmon:fresh_water",
            "pixelmon:soda_pop",
            "pixelmon:lemonade",
    };

    private static final String[] UNCOMMON_ITEMS = {
            "pixelmon:super_potion",
            "minecraft:iron_ingot",
            "pixelmon:platinum_ingot",
            "pixelmon:full_heal",
            "pixelmon:elixir",
            "pixelmon:revive",
    };

    private static final String[] RARE_ITEMS = {
            "pixelmon:hyper_potion",
            "pixelmon:fire_stone", "pixelmon:water_stone", "pixelmon:thunder_stone", "pixelmon:leaf_stone",
            "pixelmon:moon_stone", "pixelmon:sun_stone", "pixelmon:shiny_stone", "pixelmon:dusk_stone",
            "pixelmon:dawn_stone", "pixelmon:ice_stone",
            "minecraft:gold_block",
            "pixelmon:soothe_bell", "pixelmon:linking_cord",
            "pixelmon:lure_bug_weak", "pixelmon:lure_dark_weak", "pixelmon:lure_dragon_weak",
            "pixelmon:lure_electric_weak", "pixelmon:lure_fairy_weak", "pixelmon:lure_fighting_weak",
            "pixelmon:lure_fire_weak", "pixelmon:lure_flying_weak", "pixelmon:lure_ghost_weak",
            "pixelmon:lure_grass_weak", "pixelmon:lure_ground_weak", "pixelmon:lure_ice_weak",
            "pixelmon:lure_casing_weak", // "Weak Lure" (generic) -- the empty lure casing
            "pixelmon:lure_normal_weak", "pixelmon:lure_poison_weak", "pixelmon:lure_psychic_weak",
            "pixelmon:lure_rock_weak", "pixelmon:lure_steel_weak", "pixelmon:lure_water_weak",
            "pixelmon:max_revive", "pixelmon:max_elixir", "pixelmon:iron_hammer",
            // Quick Ball is added separately as a ball variant (Poke Ball item + component).
    };

    private static final String[] EPIC_ITEMS = {
            "pixelmon:max_potion",
            "pixelmon:ability_capsule", "pixelmon:ability_patch",
            "pixelmon:macho_brace",
            "pixelmon:power_weight", "pixelmon:power_bracer", "pixelmon:power_belt",
            "pixelmon:power_lens", "pixelmon:power_band", "pixelmon:power_anklet",
            "minecraft:diamond",
            "pixelmon:lure_ha_weak", "pixelmon:lure_shiny_weak",
            // Nature mints (Pixelmon ids are mint_<nature>).
            "pixelmon:mint_adamant", "pixelmon:mint_bold", "pixelmon:mint_brave", "pixelmon:mint_calm",
            "pixelmon:mint_careful", "pixelmon:mint_gentle", "pixelmon:mint_hasty", "pixelmon:mint_impish",
            "pixelmon:mint_jolly", "pixelmon:mint_lax", "pixelmon:mint_lonely", "pixelmon:mint_mild",
            "pixelmon:mint_modest", "pixelmon:mint_naive", "pixelmon:mint_naughty", "pixelmon:mint_quiet",
            "pixelmon:mint_rash", "pixelmon:mint_relaxed", "pixelmon:mint_sassy", "pixelmon:mint_serious",
            "pixelmon:mint_timid",
            "pixelmon:mach_bike", "pixelmon:acro_bike",
    };

    /** All 16 colored Poke Bags -- collectively they share one Epic weight. */
    private static final String[] POKE_BAGS = {
            "pixelmon:white_poke_bag", "pixelmon:orange_poke_bag", "pixelmon:magenta_poke_bag",
            "pixelmon:light_blue_poke_bag", "pixelmon:yellow_poke_bag", "pixelmon:lime_poke_bag",
            "pixelmon:pink_poke_bag", "pixelmon:gray_poke_bag", "pixelmon:light_gray_poke_bag",
            "pixelmon:cyan_poke_bag", "pixelmon:purple_poke_bag", "pixelmon:blue_poke_bag",
            "pixelmon:brown_poke_bag", "pixelmon:green_poke_bag", "pixelmon:red_poke_bag",
            "pixelmon:black_poke_bag",
    };

    private static final String[] LEGENDARY_ITEMS = {
            "pixelmon:full_restore",
            "pixelmon:orb",
    };

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        try {
            if (!CCDelicTweaksConfig.lootOverrideEnabled()) {
                return;
            }
            if (!TARGETS.contains(event.getName())) {
                return;
            }
            event.setTable(buildTable());
            CCDelicTweaksMod.LOGGER.debug("[CCDelicTweaks] Overrode loot table {}", event.getName());
        } catch (Throwable t) {
            // Never let a bad override break resource loading; leave the original table in place.
            CCDelicTweaksMod.LOGGER.error("[CCDelicTweaks] Failed to override loot table {}; keeping original.",
                    event.getName(), t);
        }
    }

    private static LootTable buildTable() {
        int min = CCDelicTweaksConfig.lootMinRolls();
        int max = CCDelicTweaksConfig.lootMaxRolls();
        if (min > max) {
            int tmp = min;
            min = max;
            max = tmp;
        }

        LootPool.Builder pool = LootPool.lootPool().setRolls(UniformGenerator.between(min, max));

        // Each configured weight is the WHOLE tier's relative share, split evenly across the tier's
        // items so a tier's chance depends only on its weight -- NOT on how many items it contains.
        // (Previously the weight was applied per item, so the 34-item Rare tier dwarfed the 3-item
        // Common tier.) The Poke Ball of each tier and the Poke Bag group each count as one slot.
        addTier(pool, COMMON_ITEMS, List.of(PokeBallRegistry.POKE_BALL), null, CCDelicTweaksConfig.lootCommonWeight());
        addTier(pool, UNCOMMON_ITEMS, List.of(PokeBallRegistry.GREAT_BALL), null, CCDelicTweaksConfig.lootUncommonWeight());
        addTier(pool, RARE_ITEMS, List.of(PokeBallRegistry.ULTRA_BALL, PokeBallRegistry.QUICK_BALL), null,
                CCDelicTweaksConfig.lootRareWeight());
        addTier(pool, EPIC_ITEMS, List.of(), POKE_BAGS, CCDelicTweaksConfig.lootEpicWeight());
        addTier(pool, LEGENDARY_ITEMS, List.of(), null, CCDelicTweaksConfig.lootLegendaryWeight());

        return LootTable.lootTable()
                .setParamSet(LootContextParamSets.CHEST)
                .withPool(pool)
                .build();
    }

    /**
     * Adds a full rarity tier to the pool, dividing {@code tierWeight} across its slots so the tier's
     * total weight stays proportional to {@code tierWeight} regardless of item count.
     *
     * @param items      the plain items in this tier
     * @param balls      Poke Ball variants that belong to this tier (each is one slot); may be empty
     * @param bags       an optional group of items sharing a single slot (e.g. the 16 Poke Bags), or null
     * @param tierWeight the tier's relative weight (0 disables the tier)
     */
    private static void addTier(LootPool.Builder pool, String[] items, List<RegistryValue<PokeBall>> balls,
                                String[] bags, int tierWeight) {
        if (tierWeight <= 0) {
            return;
        }
        int slots = items.length + balls.size() + (bags != null ? 1 : 0);
        if (slots <= 0) {
            return;
        }
        // Scale up before dividing so items in large tiers don't round down to 0.
        int perSlot = (int) Math.max(1L, (long) tierWeight * WEIGHT_SCALE / slots);

        for (String id : items) {
            Item item = resolve(id);
            if (item != null) {
                pool.add(LootItem.lootTableItem(item).setWeight(perSlot));
            }
        }
        for (RegistryValue<PokeBall> ball : balls) {
            pool.add(LootItem.lootTableItem(ItemRegistration.POKE_BALL.value())
                    .setWeight(perSlot)
                    .apply(SetComponentsFunction.setComponent(PixelmonDataComponents.POKE_BALL.get(), ball)));
        }
        if (bags != null && bags.length > 0) {
            int perBag = (int) Math.max(1L, (long) perSlot / bags.length);
            for (String id : bags) {
                Item item = resolve(id);
                if (item != null) {
                    pool.add(LootItem.lootTableItem(item).setWeight(perBag));
                }
            }
        }
    }

    /** Resolves an item id, returning {@code null} (with a warning) if it isn't registered. */
    private static Item resolve(String id) {
        ResourceLocation rl = ResourceLocation.parse(id);
        Item item = BuiltInRegistries.ITEM.get(rl);
        if (item == Items.AIR && !"air".equals(rl.getPath())) {
            CCDelicTweaksMod.LOGGER.warn("[CCDelicTweaks] Loot item not found, skipping: {}", id);
            return null;
        }
        return item;
    }
}
