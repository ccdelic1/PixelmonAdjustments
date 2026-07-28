package com.ccdelic.tweaks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ccdelic.tweaks.CCDelicTweaksMod;
import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.ccdelic.tweaks.spawn.EvolutionStageHelper;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon;

/**
 * Early-Stage Spawn Level Unlock.
 *
 * <p>Every wild spawn set carries a {@code minLevel}/{@code maxLevel} pair. The floor means a Pokemon
 * at the very start of its evolution line often cannot spawn low enough to be raised through its own
 * evolutions -- a set declaring 25-40 never produces anything below 25. This lowers {@code minLevel}
 * itself, once, as each spawn set is imported, so a 25-40 set becomes 3-40 (the floor is configurable).
 * {@code maxLevel}, rarity, biome and every spawn condition are untouched. See
 * {@link EvolutionStageHelper#isStartOfLine} for which Pokemon qualify.
 *
 * <p>Because this widens the spawn set's own level window rather than assigning a level, Pixelmon's
 * {@code spawn-levels-closer-to-player-levels} behaviour is preserved: {@code PlayerBasedLevels} still
 * picks a level from the player's party range and clamps it into {@code [minLevel, maxLevel]}. It just
 * now has room to land low for a low-level player instead of being forced up to the old floor.
 *
 * <h2>Why the field and not the level roll</h2>
 * The obvious hook is the {@code RandomHelper.getRandomNumberBetween(minLevel, maxLevel)} roll inside
 * {@code construct}, and that does produce a low level -- but the level does not survive. Pixelmon
 * applies spawner <em>tweaks</em> in {@code SpawnAction.doSpawn}, after the action is constructed, and
 * {@code PlayerBasedLevels} (active whenever Pixelmon's own {@code spawn-levels-closer-to-player-levels}
 * is true, which is its default) immediately overwrites it:
 *
 * <pre>{@code
 * int newLevel = getTweakedLevel(spawner, actionPokemon, ..., spawnInfo.minLevel, spawnInfo.maxLevel);
 * pixelmon.getPokemon().setLevel(newLevel);          // -> Mth.clamp(roll, minLevel, maxLevel)
 * }</pre>
 *
 * It re-derives the level from {@code spawnInfo.minLevel} and clamps to it, so a rolled level 1 was
 * pulled straight back up to 25 and the feature looked like it did nothing.
 *
 * <p>Lowering the field instead fixes that at the source, and every consumer agrees: the roll in
 * {@code construct}, the clamp in {@code PlayerBasedLevels.doTweak}, and -- importantly --
 * {@code PlayerBasedLevels.fits}, which otherwise <em>rejects</em> a spawn outright when
 * {@code spawnInfo.minLevel} is above the player's highest party level. That last one alone would
 * have kept these Pokemon away from exactly the new players the feature is meant to help.
 *
 * <p>This also matches Pixelmon's own model: {@code Stats.calculateMinMaxLevels()} already assigns a
 * natural minimum level of 1 to any form with no pre-evolution.
 *
 * <h2>Consequences of editing loaded data</h2>
 * The value is re-read from JSON on every spawn-set load, so the edit never accumulates and is undone
 * by a reload. It does mean the toggle applies when spawn sets are imported rather than instantly:
 * change the option, then {@code /reload} (or restart). Note also that if a server ever exports its
 * spawn sets to JSON while this is enabled, the lowered floor would be written into the export.
 */
@Mixin(SpawnInfoPokemon.class)
public abstract class SpawnInfoPokemonMixin {

    @Inject(method = "onImport", at = @At("RETURN"), remap = false)
    private void ccdelic$unlockEarlyStageSpawnLevels(CallbackInfo ci) {
        try {
            SpawnInfoPokemon self = (SpawnInfoPokemon) (Object) this;
            // Injected at RETURN so Pixelmon's own min/max normalisation has already run.
            if (self.minLevel <= 1 || !CCDelicTweaksConfig.earlyStageLevelUnlockEnabled()) {
                return;
            }
            // Never raise a floor: a set already at or below the configured floor keeps its own, and a
            // set whose maximum is below the floor keeps that maximum. Checked before resolving the
            // species, so spawn sets that would not change cost nothing at import time.
            int floor = Math.max(1, Math.min(CCDelicTweaksConfig.earlyStageMinLevel(), self.maxLevel));
            if (floor >= self.minLevel) {
                return;
            }
            Stats form;
            try {
                form = self.getForm();
            } catch (Throwable t) {
                // onImport runs while spawn sets deserialize, which is before Pixelmon prunes entries
                // whose species cannot resolve (a bad spec, or a disabled generation). getForm() throws
                // for those. That is unusable data rather than a failure on our side, so skip the entry
                // quietly instead of logging a stack trace for each one on every datapack load.
                return;
            }
            if (!EvolutionStageHelper.isStartOfLine(form, CCDelicTweaksConfig.earlyStageExcludeRareSpecies())) {
                return;
            }
            if (CCDelicTweaksConfig.earlyStageLogAdjustments()) {
                CCDelicTweaksMod.LOGGER.info(
                        "[PixelmonAdjustments] Early-stage level unlock: {} spawn range {}-{} -> {}-{}.",
                        EvolutionStageHelper.displayName(form), self.minLevel, self.maxLevel, floor, self.maxLevel);
            }
            self.minLevel = floor;
        } catch (Throwable t) {
            CCDelicTweaksMod.LOGGER.error(
                    "[PixelmonAdjustments] Early-stage level unlock failed; keeping Pixelmon's own spawn level range.", t);
        }
    }
}
