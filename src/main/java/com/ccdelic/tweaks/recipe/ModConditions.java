package com.ccdelic.tweaks.recipe;

import java.util.function.Supplier;

import com.ccdelic.tweaks.CCDelicTweaksMod;
import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registers this mod's recipe condition codecs (the {@code "neoforge:conditions"} {@code "type"} keys
 * referenced from datapack recipe JSON).
 */
public final class ModConditions {

    public static final DeferredRegister<MapCodec<? extends ICondition>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, CCDelicTweaksMod.MOD_ID);

    public static final Supplier<MapCodec<WoodenButtonRecipesEnabledCondition>> WOODEN_BUTTON_RECIPES_ENABLED =
            SERIALIZERS.register("wooden_button_recipes_enabled", () -> WoodenButtonRecipesEnabledCondition.CODEC);

    private ModConditions() {
    }
}
