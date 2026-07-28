package com.ccdelic.tweaks.mixin;

import java.util.ArrayList;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.ccdelic.tweaks.CCDelicTweaksMod;
import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.ccdelic.tweaks.spawn.EvolutionStageHelper;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon;
import com.pixelmonmod.pixelmon.api.world.WorldTime;
import com.pixelmonmod.pixelmon.command.impl.wiki.SpawningStrategy;

/**
 * Early-Stage Time-of-Day Unlock (/wiki display side).
 *
 * <p>Companion to {@link SpawnConditionMixin}, which is what actually removes the gate. The
 * {@code /wiki <pokemon>} Spawning section ends each spawn entry with an optional line built from
 * {@code spawnInfo.condition.times}:
 *
 * <pre>{@code
 * if (spawnInfo.condition.times != null && !spawnInfo.condition.times.isEmpty()) {
 *     sender.sendSuccess(... "pixelmon.command.wiki.spawning.time" ...);
 * }
 * }</pre>
 *
 * <p>Left alone, that line would keep telling players a Pokemon spawns only at Dusk/Night after the
 * gate has been lifted. This intercepts the two {@code times} field reads that form the guard and
 * hands back an empty list for exactly the Pokemon whose gate the spawn-side mixin bypasses, so the
 * guard fails and the line is omitted -- which is precisely how {@code /wiki} already renders a spawn
 * that never had a time restriction, so no new wording or translation key is needed.
 *
 * <p>Everything else in the entry (spec, biomes, Y range, location type, rarity, weather) is
 * untouched, and the two mixins read the same config and the same
 * {@link EvolutionStageHelper#isStartOfLine} test, so the display can never drift from the behaviour.
 * When the feature is off, or {@code update_wiki_display} is false, the real list is returned and
 * {@code /wiki} prints Pixelmon's stock output.
 *
 * <p>Note that Pixelmon's in-game Pokedex builds its spawn panel from a different source
 * ({@code Stats.getPokedexData()} via {@code SpawnInfo.getDisplayedSpawning()}) and is not affected
 * by this; only the {@code /wiki} command is.
 */
@Mixin(SpawningStrategy.class)
public abstract class SpawningStrategyMixin {

    @ModifyExpressionValue(method = "sendBiomeMessage", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lcom/pixelmonmod/pixelmon/api/spawning/conditions/SpawnCondition;times:Ljava/util/ArrayList;"),
            remap = false)
    private static ArrayList<WorldTime> ccdelic$hideUnlockedTimeRestriction(
            ArrayList<WorldTime> times, @Local SpawnInfoPokemon spawnInfo) {
        try {
            if (times != null && !times.isEmpty()
                    && CCDelicTweaksConfig.earlyStageTimeUnlockEnabled()
                    && CCDelicTweaksConfig.timeUnlockUpdateWikiDisplay()
                    && EvolutionStageHelper.isStartOfLine(
                            spawnInfo.getForm(), CCDelicTweaksConfig.timeUnlockExcludeRareSpecies())) {
                // Empty rather than null: the guard reads the field twice, and an empty list satisfies
                // the null check and then fails the isEmpty() check, so the line is skipped either way.
                return new ArrayList<>();
            }
        } catch (Throwable t) {
            CCDelicTweaksMod.LOGGER.error(
                    "[PixelmonAdjustments] /wiki spawning time display check failed; showing Pixelmon's own output.", t);
        }
        return times;
    }
}
