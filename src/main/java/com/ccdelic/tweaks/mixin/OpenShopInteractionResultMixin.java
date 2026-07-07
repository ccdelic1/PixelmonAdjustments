package com.ccdelic.tweaks.mixin;

import java.util.List;
import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ccdelic.tweaks.CCDelicTweaksMod;
import com.ccdelic.tweaks.config.CCDelicTweaksConfig;
import com.pixelmonmod.pixelmon.api.context.StoredContext;
import com.pixelmonmod.pixelmon.api.npc.interaction.result.type.OpenShopInteractionResult;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
import com.pixelmonmod.pixelmon.api.shop.ShopItem;
import com.pixelmonmod.pixelmon.init.registry.ItemRegistration;
import com.pixelmonmod.pixelmon.items.PokeBallItem;
import com.pixelmonmod.pixelmon.items.PokeBallPart;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Shopkeeper Inventory Override (Feature #2).
 *
 * <p>Intercepts every NPC shop opened through {@link OpenShopInteractionResult#handle} -- the single
 * code path all PokeMart (and daycare) shopkeepers use -- and rewrites the item list before it is
 * sent to the player: keep only the standard Poke Ball (removing Great/Ultra/Luxury/etc.), set its
 * price, and ensure a Revive is on sale at the configured price.
 *
 * <p>PokeMarts are detected by the presence of a Poke Ball item; daycare shops (stones/day-care
 * items) and vending machines (which bypass this method entirely) are therefore left alone. The
 * modification is idempotent, so it is safe whether the interaction result is a shared singleton or
 * re-parsed per interaction.
 *
 * <p>The injector never throws: any error is caught so a failed override can't break the NPC
 * interaction, and the shop is guaranteed non-empty before {@code send()} (which rejects empty lists).
 */
@Mixin(OpenShopInteractionResult.class)
public abstract class OpenShopInteractionResultMixin {

    @Shadow
    @Final
    protected List<ShopItem> shopItems;

    @Inject(method = "handle", at = @At("HEAD"), remap = false)
    private void ccdelic$overrideShop(StoredContext context, CallbackInfo ci) {
        try {
            if (!CCDelicTweaksConfig.shopOverrideEnabled()) {
                return;
            }
            List<ShopItem> items = this.shopItems;
            if (items == null || items.isEmpty()) {
                return;
            }

            boolean isPokemart = items.stream()
                    .anyMatch(it -> PokeBallPart.getPokeBall(it.itemStack()).isPresent());
            if (CCDelicTweaksConfig.shopOnlyPokemarts() && !isPokemart) {
                return;
            }

            double ballBuy = CCDelicTweaksConfig.shopPokeballBuyPrice();
            double ballSell = CCDelicTweaksConfig.shopPokeballSellPrice();

            // 1. Remove every non-standard ball type.
            if (CCDelicTweaksConfig.shopRemoveOtherBalls()) {
                items.removeIf(it -> {
                    Optional<PokeBall> ball = PokeBallPart.getPokeBall(it.itemStack());
                    return ball.isPresent() && !"poke_ball".equals(ball.get().getName());
                });
            }

            // 2. Set the standard Poke Ball's price wherever it appears.
            for (int i = 0; i < items.size(); i++) {
                ShopItem it = items.get(i);
                Optional<PokeBall> ball = PokeBallPart.getPokeBall(it.itemStack());
                if (ball.isPresent() && "poke_ball".equals(ball.get().getName())) {
                    items.set(i, new ShopItem(it.itemStack(), ballBuy, ballSell));
                }
            }

            // 3. Ensure a Revive is on sale at the configured price.
            if (CCDelicTweaksConfig.shopAddRevive()) {
                double reviveBuy = CCDelicTweaksConfig.shopReviveBuyPrice();
                double reviveSell = CCDelicTweaksConfig.shopReviveSellPrice();
                Item revive = ItemRegistration.REVIVE.value();
                boolean updated = false;
                for (int i = 0; i < items.size(); i++) {
                    ShopItem it = items.get(i);
                    if (it.itemStack().is(revive)) {
                        items.set(i, new ShopItem(it.itemStack(), reviveBuy, reviveSell));
                        updated = true;
                        break;
                    }
                }
                if (!updated) {
                    items.add(new ShopItem(new ItemStack(revive, 1), reviveBuy, reviveSell));
                }
            }

            // 4. Safety: ShopBuilder.send() rejects an empty list. If filtering emptied the shop
            //    (e.g. a custom shop that sold only non-standard balls), restore a standard Poke Ball.
            if (items.isEmpty()) {
                PokeBall standard = PokeBallRegistry.POKE_BALL.getValueUnsafe();
                if (standard != null) {
                    items.add(new ShopItem(PokeBallItem.of(standard), ballBuy, ballSell));
                }
            }

            if (CCDelicTweaksConfig.shopLogOverrides()) {
                CCDelicTweaksMod.LOGGER.info("[CCDelicTweaks] Shop override applied; {} item(s) on sale.", items.size());
            }
        } catch (Throwable t) {
            CCDelicTweaksMod.LOGGER.error("[CCDelicTweaks] Shop override failed; leaving shop unchanged.", t);
        }
    }
}
