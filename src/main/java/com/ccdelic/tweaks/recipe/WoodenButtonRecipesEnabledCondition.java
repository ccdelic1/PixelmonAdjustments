package com.ccdelic.tweaks.recipe;

import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Recipe condition gating the wooden-button crafting variants added for iron-base Poke Balls
 * (see {@code data/pixelmonadjustments/recipe/pokeball_wood_button/*.json}).
 *
 * <p>Recipes are plain datapack JSON with no config hook of their own, so each of those recipe files
 * carries a {@code "neoforge:conditions"} entry referencing this condition (registered under
 * {@code pixelmonadjustments:wooden_button_recipes_enabled|corresponding id} in {@link ModConditions}).
 * NeoForge evaluates {@link #test} whenever recipes (re)load, so toggling
 * {@code wooden_button_recipes.doWoodenButtonsForIronBases} and running {@code /reload} takes effect
 * immediately, exactly like the config-driven village chest loot table.
 */
public record WoodenButtonRecipesEnabledCondition() implements ICondition {

    public static final MapCodec<WoodenButtonRecipesEnabledCondition> CODEC =
            MapCodec.unit(WoodenButtonRecipesEnabledCondition::new);

    @Override
    public boolean test(IContext context) {
        return CCDelicTweaksConfig.doWoodenButtonsForIronBases();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "pixelmonadjustments:wooden_button_recipes_enabled";
    }
}
