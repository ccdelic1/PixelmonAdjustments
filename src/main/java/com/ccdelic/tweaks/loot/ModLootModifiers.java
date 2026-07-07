package com.ccdelic.tweaks.loot;

import java.util.function.Supplier;

import com.ccdelic.tweaks.CCDelicTweaksMod;
import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registers this mod's Global Loot Modifier serializers.
 *
 * <p>The modifier instance itself is declared in
 * {@code data/pixelmonadjustments/loot_modifiers/machine_parts.json} and listed in
 * {@code global_loot_modifiers.json}; this only registers the codec that deserializes it.
 */
public final class ModLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, CCDelicTweaksMod.MOD_ID);

    public static final Supplier<MapCodec<MachinePartLootModifier>> MACHINE_PARTS =
            SERIALIZERS.register("machine_parts", () -> MachinePartLootModifier.CODEC);

    private ModLootModifiers() {
    }
}
