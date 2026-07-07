package com.ccdelic.tweaks.item;

import com.ccdelic.tweaks.CCDelicTweaksMod;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom items added by Pixelmon: Adjustments.
 *
 * <p>{@code healer_part} and {@code pc_part} are plain crafting-component items (no block form, no
 * behavior). They drop from Healer/PC blocks (see {@code MachinePartLootModifier}) and craft back
 * into a White Healer / White PC.
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CCDelicTweaksMod.MOD_ID);

    public static final DeferredItem<Item> HEALER_PART = ITEMS.registerSimpleItem("healer_part");
    public static final DeferredItem<Item> PC_PART = ITEMS.registerSimpleItem("pc_part");

    private ModItems() {
    }
}
