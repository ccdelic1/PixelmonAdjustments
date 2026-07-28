package com.ccdelic.tweaks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.ccdelic.tweaks.CCDelicTweaksMod;
import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pixelmonmod.pixelmon.api.replacement.logic.VillagerReplacementLogic;

import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Village NPC Coexistence.
 *
 * <p>Pixelmon replaces every vanilla Villager, Wandering Trader, and Zombie Villager that naturally
 * spawns with one of its own trainer NPCs -- see {@code pixelmon:pixelmon/spawn_replacement/villager.json}
 * and {@link com.pixelmonmod.pixelmon.listener.MobSpawnReplacement}, which cancels the vanilla mob's
 * {@link EntityJoinLevelEvent} and spawns a Pixelmon NPC at the same position via
 * {@link VillagerReplacementLogic#replaceSpawn}. That cancellation is the ONLY thing preventing the
 * vanilla Villager from also existing -- the NPC spawn itself is unrelated and happens either way.
 *
 * <p>This wraps the single {@code event.setCanceled(true)} call inside {@code replaceSpawn} and, when
 * enabled and the replaced entity is specifically a vanilla {@code minecraft:villager} (not a
 * Wandering Trader or Zombie Villager -- Pixelmon still replaces those normally), skips the
 * cancellation so the vanilla Villager stays in the world alongside the Pixelmon NPC that spawns right
 * after it in the same method. When disabled (or on any error), the call passes through unchanged,
 * restoring Pixelmon's stock replace-only behavior.
 */
@Mixin(VillagerReplacementLogic.class)
public abstract class VillagerReplacementLogicMixin {

    @WrapOperation(method = "replaceSpawn", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/event/entity/EntityJoinLevelEvent;setCanceled(Z)V"),
            remap = false)
    private void ccdelic$keepVanillaVillager(EntityJoinLevelEvent event, boolean canceled, Operation<Void> original) {
        boolean keepVanilla;
        try {
            keepVanilla = CCDelicTweaksConfig.doSpawnVanillaVillagers()
                    && event.getEntity().getType() == EntityType.VILLAGER;
        } catch (Throwable t) {
            CCDelicTweaksMod.LOGGER.error(
                    "[PixelmonAdjustments] Village NPC coexistence check failed; using default Pixelmon behavior.", t);
            keepVanilla = false;
        }
        if (keepVanilla) {
            // Skip the cancellation -- the vanilla Villager stays, and replaceSpawn still spawns the
            // Pixelmon NPC at the same position right after this call, so both coexist.
            return;
        }
        original.call(event, canceled);
    }
}
