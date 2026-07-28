package com.ccdelic.tweaks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.ccdelic.tweaks.CCDelicTweaksMod;
import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.ccdelic.tweaks.spawn.EvolutionStageHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pixelmonmod.pixelmon.api.spawning.SpawnInfo;
import com.pixelmonmod.pixelmon.api.spawning.SpawnLocation;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon;
import com.pixelmonmod.pixelmon.api.spawning.conditions.SpawnCondition;

import net.minecraft.world.level.Level;

/**
 * Early-Stage Time-of-Day Unlock (spawn side).
 *
 * <p>{@link SpawnCondition#fits} runs a spawn's declared restrictions in sequence, and one of them is
 * {@code doesWorldTimeFit(world)}, which fails the whole condition unless the world's current
 * {@code WorldTime} is in the condition's {@code times} list. That is what confines a Hoothoot spawn
 * set declaring {@code "times": ["DUSK", "NIGHT"]} to the night.
 *
 * <p>This wraps that one call and short-circuits it to {@code true} for Pokemon at the start of their
 * evolution line (see {@link EvolutionStageHelper#isStartOfLine}), so those spawns become available at
 * any hour. Nothing else in {@code fits} is touched -- biome, weather, Y range, light level, moon
 * phase, structure, dimension, needed blocks and research checks all still run and still fail the
 * spawn exactly as before. Real-world-clock times ({@code doesRealTimeFit}) are also left alone, since
 * those are used for event spawns rather than day/night gating.
 *
 * <h2>Why the condition identity check matters</h2>
 * The same {@code SpawnCondition} class backs both a spawn's positive {@code condition} and its
 * {@code anticondition}, and for an anticondition {@code fits() == true} means "block this spawn".
 * Forcing the time check to pass there would make an anticondition match MORE often and remove
 * spawns rather than add them -- the exact opposite of this feature. So the bypass is applied only
 * when this condition is the spawn's own primary {@code condition}. Composite conditions and the
 * Better Spawner's global composite condition are likewise left untouched. In Pixelmon 9.3.16's
 * shipped spawn data that covers every time-gated spawn there is: 2440 spawnInfos declare
 * {@code condition.times}, none declare {@code anticondition.times}, and no set uses a composite
 * condition at all. It also keeps the runtime behaviour aligned with what {@code /wiki} displays,
 * which reads {@code spawnInfo.condition.times}.
 *
 * <p>The handler is wrapped in a try/catch and the feature is off by default, so any failure falls
 * back to Pixelmon's own time check.
 */
@Mixin(SpawnCondition.class)
public abstract class SpawnConditionMixin {

    @WrapOperation(method = "fits", at = @At(value = "INVOKE",
            target = "Lcom/pixelmonmod/pixelmon/api/spawning/conditions/SpawnCondition;doesWorldTimeFit(Lnet/minecraft/world/level/Level;)Z"),
            remap = false)
    private boolean ccdelic$ignoreTimeOfDayForEarlyStages(
            SpawnCondition self, Level world, Operation<Boolean> original,
            SpawnInfo spawnInfo, SpawnLocation spawnLocation) {
        try {
            // Ordered cheapest-first: this runs for every candidate spawn the Better Spawner evaluates.
            // A condition with no times list has no gate to remove, and doesWorldTimeFit would just
            // return true anyway, so bail out before doing any species work.
            if (self.times != null && !self.times.isEmpty()
                    && CCDelicTweaksConfig.earlyStageTimeUnlockEnabled()
                    && spawnInfo instanceof SpawnInfoPokemon pokemonInfo
                    // Positive conditions only -- see the class javadoc for why anticonditions must not
                    // be relaxed. Reference equality is the point: it identifies this exact block.
                    && spawnInfo.condition == self
                    && EvolutionStageHelper.isStartOfLine(
                            pokemonInfo.getForm(), CCDelicTweaksConfig.timeUnlockExcludeRareSpecies())) {
                if (CCDelicTweaksConfig.timeUnlockLogAdjustments() && !original.call(self, world)) {
                    CCDelicTweaksMod.LOGGER.info(
                            "[PixelmonAdjustments] Early-stage time unlock: allowing {} to spawn outside its {} window.",
                            EvolutionStageHelper.displayName(pokemonInfo.getForm()), self.times);
                }
                return true;
            }
        } catch (Throwable t) {
            CCDelicTweaksMod.LOGGER.error(
                    "[PixelmonAdjustments] Early-stage time unlock failed; using Pixelmon's own time check.", t);
        }
        return original.call(self, world);
    }
}
