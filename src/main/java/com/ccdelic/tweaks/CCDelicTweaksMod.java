package com.ccdelic.tweaks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.ccdelic.tweaks.item.ModItems;
import com.ccdelic.tweaks.legendary.LegendarySpawnListener;
import com.ccdelic.tweaks.loot.ModLootModifiers;
import com.ccdelic.tweaks.loot.VillageChestLoot;
import com.ccdelic.tweaks.raid.RaidAllyFilter;
import com.pixelmonmod.pixelmon.Pixelmon;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Entry point for Pixelmon: Adjustments, a Pixelmon side-mod.
 *
 * <p>Features:
 * <ol>
 *   <li>Legendary Spawn Anti-Repeat -- stops the same legendary spawning back-to-back.</li>
 *   <li>Shopkeeper Inventory Override -- PokeMarts sell only the standard Poke Ball, plus Revives.</li>
 *   <li>Custom Village Chest Loot -- curated tiered loot for arena/waypoint chests.</li>
 *   <li>Smart Raid Ally Selection -- BST-matched, type-safe AI raid partners.</li>
 *   <li>Healer/PC Break Drops -- custom parts (non-silk-touch) plus crafting recipes.</li>
 * </ol>
 *
 * <p>Every feature is defensively wrapped so a failure in this mod can never crash Pixelmon or the
 * game -- listeners/mixins catch their own errors and fall back to vanilla behavior.
 *
 * <p>The {@code @Mod} id must match {@code mod_id} in {@code gradle.properties} and the
 * {@code modId} in {@code src/main/resources/META-INF/neoforge.mods.toml}.
 */
@Mod(CCDelicTweaksMod.MOD_ID)
public class CCDelicTweaksMod {

    public static final String MOD_ID = "pixelmonadjustments";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public CCDelicTweaksMod(IEventBus modEventBus, ModContainer container) {
        LOGGER.info("[PixelmonAdjustments] Mod constructor called");

        try {
            // Server-side config, written to PixelmonAdjustments.toml (instead of the default
            // pixelmonadjustments-server.toml).
            container.registerConfig(ModConfig.Type.SERVER, CCDelicTweaksConfig.SPEC, "PixelmonAdjustments.toml");

            // Registries (custom items + global loot modifier serializer) on the mod event bus.
            ModItems.ITEMS.register(modEventBus);
            ModLootModifiers.SERIALIZERS.register(modEventBus);

            // Village chest loot override -- LootTableLoadEvent is fired on the NeoForge game bus.
            NeoForge.EVENT_BUS.register(new VillageChestLoot());
        } catch (Throwable t) {
            LOGGER.error("[PixelmonAdjustments] Error during mod construction", t);
        }

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Register on Pixelmon's own event bus (separate from NeoForge's). Pixelmon is loaded
        // before us (ordering="AFTER" in mods.toml), so the bus is available here.
        event.enqueueWork(() -> {
            try {
                Pixelmon.EVENT_BUS.register(new LegendarySpawnListener());
                Pixelmon.EVENT_BUS.register(new RaidAllyFilter());
                LOGGER.info("[PixelmonAdjustments] Registered Pixelmon event listeners");
            } catch (Throwable t) {
                LOGGER.error("[PixelmonAdjustments] Failed to register Pixelmon event listeners", t);
            }
        });
    }
}
