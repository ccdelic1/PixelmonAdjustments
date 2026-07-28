package com.ccdelic.tweaks.spawn;

import java.util.Locale;

import com.ccdelic.tweaks.CCDelicTweaksMod;
import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.pixelmonmod.pixelmon.api.events.spawning.SpawnEvent;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.spawning.SpawnLocation;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Alerts the single nearest player when a wild Ultra Beast spawns.
 *
 * <p>Registered on Pixelmon's own event bus ({@code Pixelmon.EVENT_BUS}). Ultra Beasts are not
 * flagged {@code isLegendary()} in Pixelmon's species data -- they carry only the {@code ultrabeast}
 * form tag -- so they spawn through the ordinary wild-spawning pipeline rather than
 * {@link com.pixelmonmod.pixelmon.api.events.spawning.LegendarySpawnEvent}. {@link SpawnEvent} is the
 * generic hook that fires for every spawn in that pipeline (Pokemon or otherwise), so we filter down
 * to Ultra Beasts ourselves.
 *
 * <p>The event fires before the entity is actually placed in the world -- its position is set
 * immediately afterward in {@code SpawnAction.doSpawn} -- so the spawn coordinates are read from the
 * event's {@code spawnLocation} rather than the entity itself, and the nearest player is found
 * directly against that location instead of the (not-yet-positioned) entity.
 */
public class UltraBeastSpawnListener {

    @SubscribeEvent
    public void onSpawn(SpawnEvent event) {
        try {
            if (!CCDelicTweaksConfig.doUltraBeastNotification()) {
                return;
            }
            // Best effort: skip if already canceled by an earlier listener.
            if (event.isCanceled()) {
                return;
            }
            Entity entity = event.action.getOrCreateEntity();
            if (!(entity instanceof PixelmonEntity pixelmonEntity)) {
                return;
            }
            Species species = pixelmonEntity.getSpecies();
            if (species == null || !species.isUltraBeast()) {
                return;
            }

            SpawnLocation spawnLocation = event.action.spawnLocation;
            BlockPos pos = spawnLocation.location.pos;
            Player nearest = spawnLocation.location.world.getNearestPlayer(
                    pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, -1.0, false);
            if (!(nearest instanceof ServerPlayer serverPlayer)) {
                return;
            }

            MutableComponent speciesName = Component
                    .translatable("pixelmon." + species.getName().toLowerCase(Locale.ROOT))
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
            MutableComponent message = Component.literal("You feel a distorted presence... ")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    .append(speciesName)
                    .append(Component.literal(" is nearby...").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));

            serverPlayer.sendSystemMessage(message);
        } catch (Throwable t) {
            // Never let this alert disrupt a Pixelmon spawn.
            CCDelicTweaksMod.LOGGER.error("[PixelmonAdjustments] Failed to send Ultra Beast spawn alert", t);
        }
    }
}
