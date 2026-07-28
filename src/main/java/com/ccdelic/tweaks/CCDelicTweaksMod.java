package com.ccdelic.tweaks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.ccdelic.tweaks.item.ModItems;
import com.ccdelic.tweaks.loot.ModLootModifiers;
import com.ccdelic.tweaks.loot.VillageChestLoot;
import com.ccdelic.tweaks.player.StarterKitListener;
import com.ccdelic.tweaks.raid.RaidAllyFilter;
import com.ccdelic.tweaks.recipe.ModConditions;
import com.ccdelic.tweaks.spawn.UltraBeastSpawnListener;
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
 *   <li>Legendary Spawn Coordinate Broadcast -- announces spawn X/Y/Z to all players in chat.</li>
 *   <li>Ultra Beast Spawn Alert -- warns the single nearest player when one spawns nearby.</li>
 *   <li>First-Join Starter Kit -- one-time item grant the very first time a player joins.</li>
 *   <li>Village NPC Coexistence -- vanilla Villagers spawn alongside Pixelmon's replacement NPCs.</li>
 *   <li>Wooden Button Poke Ball Recipes -- iron-base Poke Balls also craft with a wooden button.</li>
 *   <li>Early-Stage Spawn Level Unlock -- single-stage and first-stage Pokemon lose their spawn level
 *       floor (25-40 becomes 3-40). Off by default.</li>
 *   <li>Early-Stage Time-of-Day Unlock -- single-stage and first-stage Pokemon spawn at any hour, with
 *       {@code /wiki} updated to match. Off by default.</li>
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
            // COMMON, not SERVER: a SERVER config is per-world and is only written when a world is
            // created (<world>/serverconfig/). COMMON is written to config/PixelmonAdjustments.toml
            // during mod loading, so the file exists as soon as the game starts and one set of
            // settings applies to every world. It is also loaded before datapacks, which the
            // early-stage spawn features rely on -- they read config while spawn sets import.
            container.registerConfig(ModConfig.Type.COMMON, CCDelicTweaksConfig.SPEC, "PixelmonAdjustments.toml");

            // Registries (custom items + global loot modifier + recipe condition serializers) on the
            // mod event bus.
            ModItems.ITEMS.register(modEventBus);
            ModLootModifiers.SERIALIZERS.register(modEventBus);
            ModConditions.SERIALIZERS.register(modEventBus);

            // Village chest loot override -- LootTableLoadEvent is fired on the NeoForge game bus.
            NeoForge.EVENT_BUS.register(new VillageChestLoot());

            // First-join starter kit -- PlayerLoggedInEvent is fired on the NeoForge game bus.
            NeoForge.EVENT_BUS.register(new StarterKitListener());
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
                Pixelmon.EVENT_BUS.register(new RaidAllyFilter());
                Pixelmon.EVENT_BUS.register(new UltraBeastSpawnListener());
                LOGGER.info("[PixelmonAdjustments] Registered Pixelmon event listeners");
            } catch (Throwable t) {
                LOGGER.error("[PixelmonAdjustments] Failed to register Pixelmon event listeners", t);
            }
        });
    }
}
