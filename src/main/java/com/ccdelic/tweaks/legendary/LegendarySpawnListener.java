package com.ccdelic.tweaks.legendary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.pixelmonmod.pixelmon.api.events.spawning.LegendarySpawnEvent;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;

import net.neoforged.bus.api.SubscribeEvent;

/**
 * Records successful legendary spawns into {@link LegendaryRecentTracker}.
 *
 * <p>Registered on Pixelmon's own event bus ({@code Pixelmon.EVENT_BUS}).
 * {@link LegendarySpawnEvent.DoSpawn} fires right before a (non-boss) legendary entity is added to
 * the world, so listening here means only spawns that actually happen are counted -- a spawn pass
 * that finds no location, chooses no player, or is otherwise aborted never fires this event and is
 * correctly not recorded.
 *
 * <p>Note: listener order on the bus is not guaranteed, so if a later listener cancels the spawn we
 * may still record it. This is an accepted best-effort imprecision for a soft anti-repeat feature.
 */
public class LegendarySpawnListener {

    private static final Logger LOGGER = LogManager.getLogger("pixelmonadjustments");

    @SubscribeEvent
    public void onLegendaryDoSpawn(LegendarySpawnEvent.DoSpawn event) {
        try {
            if (!CCDelicTweaksConfig.antiRepeatEnabled()) {
                return;
            }
            // Best effort: skip if already canceled by an earlier listener.
            if (event.isCanceled()) {
                return;
            }
            Species species = event.getLegendary();
            if (species == null) {
                return;
            }
            String name = species.getName();
            LegendaryRecentTracker.record(name);
            if (CCDelicTweaksConfig.logFiltered()) {
                LOGGER.info("[PixelmonAdjustments] Recorded legendary spawn '{}'. Recent history: {}",
                        name, LegendaryRecentTracker.snapshot());
            }
        } catch (Throwable t) {
            // Never let our bookkeeping disrupt a Pixelmon spawn.
            LOGGER.error("[PixelmonAdjustments] Failed to record legendary spawn", t);
        }
    }
}
