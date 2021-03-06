/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.core.item;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.api.entity.projectile.FishingBobber;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.event.tracking.PhaseTracker;

import javax.annotation.Nullable;

@Mixin(FishingRodItem.class)
public abstract class FishingRodItemMixin {

    @Nullable private FishingBobberEntity impl$fishHook;

    @Inject(method = "use", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/entity/projectile/FishingBobberEntity;retrieve(Lnet/minecraft/item/ItemStack;)I"), cancellable = true)
    private void cancelHookRetraction(World world, PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult<ItemStack>> cir) {
        if (player.fishing != null) {
            // Event was cancelled
            cir.setReturnValue(new ActionResult<>(ActionResultType.SUCCESS, player.getItemInHand(hand)));
        }
    }

    @Inject(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V", ordinal = 1),
            cancellable = true)
    private void onThrowEvent(World world, PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult<ItemStack>> cir) {
        if (world.isClientSide) {
            // Only fire event on server-side to avoid crash on client
            return;
        }
        ItemStack itemstack = player.getItemInHand(hand);
        int k = EnchantmentHelper.getFishingSpeedBonus(itemstack);
        int j = EnchantmentHelper.getFishingLuckBonus(itemstack);
        FishingBobberEntity fishHook = new FishingBobberEntity(player, world, j, k);
        PhaseTracker.getCauseStackManager().pushCause(player);
        if (SpongeCommon.postEvent(SpongeEventFactory.createFishingEventStart(PhaseTracker.getCauseStackManager().getCurrentCause(), (FishingBobber) fishHook))) {
            fishHook.remove(); // Bye
            cir.setReturnValue(new ActionResult<>(ActionResultType.SUCCESS, player.getItemInHand(hand)));
        } else {
            this.impl$fishHook = fishHook;
        }
        PhaseTracker.getCauseStackManager().popCause();
    }

    @Redirect(method = "use", at = @At(value = "NEW", target = "net/minecraft/entity/projectile/FishingBobberEntity"))
    private FishingBobberEntity onNewEntityFishHook(PlayerEntity p_i50220_1_, World p_i50220_2_, int p_i50220_3_, int p_i50220_4_) {
        // Use the fish hook we created for the event
        FishingBobberEntity fishHook = this.impl$fishHook;
        this.impl$fishHook = null;
        return fishHook;
    }

}
