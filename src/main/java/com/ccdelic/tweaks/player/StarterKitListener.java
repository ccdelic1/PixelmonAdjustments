package com.ccdelic.tweaks.player;

import com.ccdelic.tweaks.CCDelicTweaksMod;
import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.pixelmonmod.api.registry.RegistryValue;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
import com.pixelmonmod.pixelmon.init.registry.ItemRegistration;
import com.pixelmonmod.pixelmon.init.registry.PixelmonDataComponents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;

/**
 * Grants a one-time starter kit the very first time a player ever logs into the server.
 *
 * <p>Registered on the NeoForge event bus. "First time" is tracked with a boolean flag inside the
 * {@link Player#PERSISTED_NBT_TAG} sub-compound of the player's persistent data, so it survives both
 * server restarts and player death (see {@link #markReceived}). The flag is set before the items are
 * handed out so a crash mid-grant can never cause a re-grant on the next login.
 *
 * <p>Note that Pixelmon's own {@code FirstJoinInteractionEventLogic} writes its {@code
 * pixelmon_first_join} flag at the top level instead, which does not survive death -- that is
 * tolerable for repeating a line of NPC dialogue, but not for a one-time item grant.
 */
public class StarterKitListener {

    private static final String FIRST_JOIN_FLAG = "pixelmonadjustments_starter_kit_given";

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        try {
            if (!CCDelicTweaksConfig.doStarterKit()) {
                return;
            }
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            if (hasReceivedKit(player)) {
                return;
            }
            markReceived(player);

            give(player, "pixelmon:red_healer", 1);
            give(player, "pixelmon:red_pc", 1);
            give(player, "minecraft:stone_pickaxe", 1);
            give(player, "minecraft:bread", 16);
            giveBall(player, PokeBallRegistry.POKE_BALL, 10);
            giveBall(player, PokeBallRegistry.GREAT_BALL, 5);
            give(player, "pixelmon:potion", 5);
        } catch (Throwable t) {
            CCDelicTweaksMod.LOGGER.error("[PixelmonAdjustments] Failed to grant starter kit", t);
        }
    }

    /**
     * @return whether this player has already been given the kit, checking the persisted sub-tag and,
     *         for players who joined before the persistence fix, the old top-level flag.
     */
    private static boolean hasReceivedKit(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (root.getCompound(Player.PERSISTED_NBT_TAG).getBoolean(FIRST_JOIN_FLAG)) {
            return true;
        }
        if (root.getBoolean(FIRST_JOIN_FLAG)) {
            // Legacy flag from before the fix -- promote it so it survives this player's next death.
            markReceived(player);
            return true;
        }
        return false;
    }

    /**
     * Records the grant inside {@link Player#PERSISTED_NBT_TAG}.
     *
     * <p>This sub-compound is the only part of {@link ServerPlayer#getPersistentData()} that NeoForge
     * carries across a respawn ({@code ServerPlayer.restoreFrom} copies just this key). A flag written
     * at the top level is silently dropped when the player dies, and since the player is then saved
     * from the post-death entity, the kit would be granted again on their next login -- once per
     * death, indefinitely.
     */
    private static void markReceived(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        // getCompound() returns a detached empty tag when the key is absent, so it must be put back.
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(FIRST_JOIN_FLAG, true);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private static void give(ServerPlayer player, String itemId, int count) {
        Item item = resolve(itemId);
        if (item != null) {
            giveStack(player, new ItemStack(item, count));
        }
    }

    private static void giveBall(ServerPlayer player, RegistryValue<PokeBall> ball, int count) {
        ItemStack stack = new ItemStack(ItemRegistration.POKE_BALL.value(), count);
        stack.set(PixelmonDataComponents.POKE_BALL.get(), ball);
        giveStack(player, stack);
    }

    /** Adds a stack to the player's inventory, dropping any leftover at their feet if it doesn't fit. */
    private static void giveStack(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    /** Resolves an item id, returning {@code null} (with a warning) if it isn't registered. */
    private static Item resolve(String id) {
        ResourceLocation rl = ResourceLocation.parse(id);
        Item item = BuiltInRegistries.ITEM.get(rl);
        if (item == Items.AIR && !"air".equals(rl.getPath())) {
            CCDelicTweaksMod.LOGGER.warn("[PixelmonAdjustments] Starter kit item not found, skipping: {}", id);
            return null;
        }
        return item;
    }
}
