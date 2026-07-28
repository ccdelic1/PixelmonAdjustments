package com.ccdelic.tweaks.spawn;

import java.util.List;

import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.pokemon.species.tags.FormTags;

/**
 * Shared evolution-stage test for the features that relax spawn restrictions on Pokemon at the start
 * of their evolution line (the spawn level floor and the time-of-day gate).
 *
 * <p>Kept out of the mixin classes deliberately: a mixin is merged into its target at class-load time
 * and is not itself a loadable class, so mixins cannot call each other's helpers. Both mixins call
 * into here instead.
 */
public final class EvolutionStageHelper {

    private EvolutionStageHelper() {
    }

    /**
     * Tests whether a form sits at the start of its evolution line, which is the single condition
     * that captures both groups these features target:
     * <ul>
     *   <li><b>Single-stage</b> -- never evolves and nothing evolves into it (e.g. Tauros).</li>
     *   <li><b>First-stage</b> -- can evolve but nothing evolves into it (e.g. Bulbasaur).</li>
     * </ul>
     * Both have an empty pre-evolution list, while middle and final stages (Ivysaur, Venusaur) do not.
     * Pixelmon uses the same "no pre-evolution" rule in {@code Stats.calculateMinMaxLevels()} when it
     * decides a species' own natural minimum level.
     *
     * @param form                the spawning form; {@code null} returns false
     * @param excludeRareSpecies  when true, Legendaries, Mythicals, Ultra Beasts and Paradox Pokemon
     *                            are reported as ineligible even though they are single-stage
     * @return true if the form is eligible for the relaxed spawn rules
     */
    public static boolean isStartOfLine(Stats form, boolean excludeRareSpecies) {
        if (form == null) {
            return false;
        }
        try {
            List<Species> preEvolutions = form.getPreEvolutions();
            if (preEvolutions == null || !preEvolutions.isEmpty()) {
                return false; // Has a pre-evolution -> middle or final stage -> leave it alone.
            }
        } catch (Throwable t) {
            return false; // Missing/unresolvable evolution data -> leave it alone.
        }
        if (excludeRareSpecies) {
            FormTags tags = form.getTags();
            if (tags != null && (tags.isLegendary() || tags.isUltraBeast() || tags.isParadox())) {
                return false; // isLegendary() already covers Mythicals.
            }
        }
        return true;
    }

    /** Best-effort species name for debug logging; never throws. */
    public static String displayName(Stats form) {
        if (form == null) {
            return "unknown";
        }
        try {
            Species species = form.getParentSpecies();
            if (species != null) {
                return species.getName();
            }
            return String.valueOf(form.getName());
        } catch (Throwable t) {
            return "unknown";
        }
    }
}
