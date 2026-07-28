package com.ccdelic.tweaks.legendary;

import java.util.Locale;

import com.ccdelic.tweaks.CCDelicTweaksMod;
import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.spawning.SpawnLocation;
import com.pixelmonmod.pixelmon.api.util.helpers.BiomeHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Builds the legendary spawn announcement and records successful legendary spawns for the anti-repeat
 * feature. Both are driven from {@link com.ccdelic.tweaks.mixin.SpawnActionPokemonMixin} rather than
 * from a listener on {@code LegendarySpawnEvent.DoSpawn}.
 *
 * <h2>Why not the event</h2>
 * Pixelmon posts {@code LegendarySpawnEvent.DoSpawn} near the top of
 * {@code SpawnActionPokemon.doSpawn}, <em>before</em> it calls {@code super.doSpawn()} -- which is
 * where the chunk-loaded, world-border, spawn-tag-interval and generic {@code SpawnEvent} checks live,
 * any of which can still abort the spawn. Announcing (or recording) on the event therefore reports
 * legendaries that never actually appear, and poisons the anti-repeat history with them. Pixelmon's
 * own announcement deliberately sits after those checks, so this hooks the same place.
 *
 * <p>Splitting the two into separate hook points is intentional: the announcement replaces Pixelmon's
 * message in-place, so it inherits Pixelmon's {@code isDisplayLegendaryGlobalMessage()} gate and stays
 * silent when an admin has turned legendary announcements off in Pixelmon's own config. The
 * anti-repeat record must NOT inherit that gate -- history has to be kept whether or not spawns are
 * announced -- so it runs off the successful return of {@code doSpawn} instead.
 */
public final class LegendarySpawnAnnouncer {

    private LegendarySpawnAnnouncer() {
    }

    /**
     * Builds the replacement for Pixelmon's biome-only "X has spawned in a Y biome!" broadcast:
     * "X has spawned in the Y biome at x:.., y:.., z:.. in &lt;dimension&gt;!".
     *
     * <p>Pixelmon's own line is hardcoded rather than exposed through an event, so folding the
     * coordinates into a single combined message -- instead of printing a second line underneath it --
     * means substituting the component at its one broadcast call site. The {@code chat.type.announcement}
     * "[Pixelmon] ..." wrapper is applied by that call site, so it is not repeated here.
     *
     * @return the replacement sentence, or {@code null} if it could not be built (caller keeps
     *         Pixelmon's original message)
     */
    public static Component buildAnnouncement(Pokemon pokemon, SpawnLocation spawnLocation) {
        if (pokemon == null || spawnLocation == null || spawnLocation.location == null) {
            return null;
        }
        Species species = pokemon.getSpecies();
        if (species == null) {
            return null;
        }
        BlockPos pos = spawnLocation.location.pos;
        ResourceLocation dimension = spawnLocation.location.world.dimension().location();
        Component biomeName = BiomeHelper.getLocalizedBiomeName(spawnLocation.getBiome());

        MutableComponent speciesName = Component
                .translatable("pixelmon." + species.getName().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.GREEN);
        return Component.empty()
                .append(speciesName)
                .append(Component.literal(" has spawned in the "))
                .append(biomeName)
                .append(Component.literal(String.format(" biome at x:%d, y:%d, z:%d in %s!",
                        pos.getX(), pos.getY(), pos.getZ(), dimension)))
                .withStyle(ChatFormatting.GREEN);
    }

    /**
     * Records a legendary that has actually been added to the world, so the anti-repeat filter can
     * exclude it from the next few legendary rolls. No-op when the feature is disabled.
     */
    public static void recordSpawn(Species species) {
        if (species == null || !CCDelicTweaksConfig.antiRepeatEnabled()) {
            return;
        }
        String name = species.getName();
        LegendaryRecentTracker.record(name);
        if (CCDelicTweaksConfig.logFiltered()) {
            CCDelicTweaksMod.LOGGER.info("[PixelmonAdjustments] Recorded legendary spawn '{}'. Recent history: {}",
                    name, LegendaryRecentTracker.snapshot());
        }
    }
}
