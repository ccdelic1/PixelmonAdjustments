package com.ccdelic.tweaks.loot;

import com.ccdelic.tweaks.CCDelicTweaksMod;
import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.ccdelic.tweaks.item.ModItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pixelmonmod.pixelmon.blocks.machines.HealerBlock;
import com.pixelmonmod.pixelmon.blocks.machines.PCBlock;
import com.pixelmonmod.pixelmon.init.registry.ItemRegistration;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Global Loot Modifier that turns the default Healer/PC break drop into a custom part.
 *
 * <p>Pixelmon's block loot tables already do the right thing: with Silk Touch they drop the block
 * itself, otherwise they drop an aluminum ingot (and explosions drop nothing). Rather than rebuild
 * those tables -- which would require reconstructing the Silk-Touch enchantment predicate against
 * the registry -- this GLM runs on the <em>generated</em> loot and simply swaps any aluminum ingot
 * dropped by a Healer or PC block for the matching part. Silk-Touch drops (the block, no aluminum)
 * and explosion drops (empty) are therefore left untouched automatically.
 *
 * <p>Honors the {@code machine_parts.enabled} config toggle: when disabled it returns the loot
 * unchanged (vanilla aluminum). Fully guarded so it can never break loot generation.
 */
public class MachinePartLootModifier extends LootModifier {

    public static final MapCodec<MachinePartLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, MachinePartLootModifier::new));

    public MachinePartLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        try {
            if (!CCDelicTweaksConfig.machinePartsEnabled()) {
                return generatedLoot;
            }

            BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
            if (state == null) {
                return generatedLoot; // Not a block break.
            }

            Block block = state.getBlock();
            Item replacement;
            if (block instanceof HealerBlock) {
                replacement = ModItems.HEALER_PART.get();
            } else if (block instanceof PCBlock) {
                replacement = ModItems.PC_PART.get();
            } else {
                return generatedLoot; // Not one of our target blocks.
            }

            Item aluminum = ItemRegistration.ALUMINUM_INGOT.value();
            boolean changed = false;
            ObjectArrayList<ItemStack> out = new ObjectArrayList<>(generatedLoot.size());
            for (ItemStack stack : generatedLoot) {
                if (stack.is(aluminum)) {
                    out.add(new ItemStack(replacement, stack.getCount()));
                    changed = true;
                } else {
                    out.add(stack);
                }
            }
            return changed ? out : generatedLoot;
        } catch (Throwable t) {
            CCDelicTweaksMod.LOGGER.error("[PixelmonAdjustments] Machine-part loot modifier error; using original drops.", t);
            return generatedLoot;
        }
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
