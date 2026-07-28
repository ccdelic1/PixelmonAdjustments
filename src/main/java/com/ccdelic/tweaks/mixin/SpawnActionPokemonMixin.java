package com.ccdelic.tweaks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.ccdelic.tweaks.CCDelicTweaksMod;
import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.ccdelic.tweaks.legendary.LegendarySpawnAnnouncer;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnActionPokemon;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;

/**
 * Legendary Spawn Coordinate Broadcast, and the anti-repeat write path.
 *
 * <p>Both hooks live in {@code SpawnActionPokemon.doSpawn} because that is the only place where a
 * legendary spawn is known to have actually succeeded. {@code LegendarySpawnEvent.DoSpawn} -- the
 * obvious API hook -- is posted earlier in the same method, before {@code super.doSpawn()} runs the
 * chunk-loaded, world-border, spawn-tag-interval and {@code SpawnEvent} checks that can still abort
 * the spawn, so anything driven from it can report a legendary that never appears.
 *
 * <ol>
 *   <li><b>Broadcast.</b> Pixelmon emits a hardcoded biome-only "X has spawned in a Y biome!" chat
 *       line; coordinates go to the server console only. Rather than adding a second line underneath
 *       it, this replaces the component at that one {@code broadcastSystemMessage} call site with the
 *       combined coordinate version from {@link LegendarySpawnAnnouncer#buildAnnouncement}. Because
 *       the substitution happens inside Pixelmon's own message block, it automatically inherits that
 *       block's {@code isDisplayLegendaryGlobalMessage()} gate -- a server that has turned legendary
 *       announcements off in Pixelmon's config stays silent instead of having them reintroduced here.
 *       The separate console-only coordinate log Pixelmon writes is untouched.</li>
 *   <li><b>Anti-repeat record.</b> Deliberately hung off the method's return value rather than the
 *       broadcast, so history is still recorded on servers that have legendary announcements
 *       disabled. A non-null return means the entity was added to the world.</li>
 * </ol>
 *
 * <p>{@code doSpawn} is matched by full descriptor: the class also carries a synthetic bridge
 * {@code doSpawn} returning {@code Entity}, and matching on the bare name would target both -- which
 * for the return-value hook would record every legendary twice.
 *
 * <p>Both handlers swallow their own errors, and the broadcast falls back to Pixelmon's original
 * message, so a failure here can never break a spawn.
 */
@Mixin(SpawnActionPokemon.class)
public abstract class SpawnActionPokemonMixin {

    private static final String DO_SPAWN =
            "doSpawn(Lcom/pixelmonmod/pixelmon/api/spawning/AbstractSpawner;)"
                    + "Lcom/pixelmonmod/pixelmon/entities/pixelmon/PixelmonEntity;";

    @WrapOperation(method = DO_SPAWN, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"),
            remap = false)
    private void ccdelic$addCoordsToLegendaryBroadcast(
            PlayerList instance, Component message, boolean bypassPlayerPermissionLevel, Operation<Void> original) {
        Component toSend = message;
        try {
            if (CCDelicTweaksConfig.doLegendaryCoordinates()) {
                SpawnActionPokemon self = (SpawnActionPokemon) (Object) this;
                Component replacement = LegendarySpawnAnnouncer.buildAnnouncement(self.pokemon, self.spawnLocation);
                if (replacement != null) {
                    toSend = replacement;
                }
            }
        } catch (Throwable t) {
            CCDelicTweaksMod.LOGGER.error(
                    "[PixelmonAdjustments] Failed to build legendary coordinate broadcast; using Pixelmon's own message.", t);
            toSend = message;
        }
        original.call(instance, toSend, bypassPlayerPermissionLevel);
    }

    @ModifyReturnValue(method = DO_SPAWN, at = @At("RETURN"), remap = false)
    private PixelmonEntity ccdelic$recordSuccessfulLegendary(PixelmonEntity spawned) {
        try {
            // Null return == the spawn was aborted. The legendary/boss test mirrors Pixelmon's own
            // gate on LegendarySpawnEvent.DoSpawn, so exactly the same spawns are tracked.
            if (spawned != null && spawned.isLegendary() && !spawned.isBossPokemon()) {
                LegendarySpawnAnnouncer.recordSpawn(spawned.getSpecies());
            }
        } catch (Throwable t) {
            CCDelicTweaksMod.LOGGER.error("[PixelmonAdjustments] Failed to record legendary spawn", t);
        }
        return spawned;
    }
}
