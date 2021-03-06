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
package org.spongepowered.common.mixin.movementcheck.network.play;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.common.config.SpongeGameConfigs;

@Mixin(ServerPlayNetHandler.class)
public abstract class ServerPlayNetHandlerMixin_MovementCheck {

    @Shadow public ServerPlayerEntity player;

    @Redirect(method = "handleMovePlayer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;isChangingDimension()Z", ordinal = 0))
    private boolean movementCheck$onPlayerMovedTooQuicklyCheck(final ServerPlayerEntity player) {
        if (SpongeGameConfigs.getForWorld(this.player.level).get().movementChecks.player.movedTooQuickly) {
            return player.isChangingDimension();
        }
        return true; // The 'moved too quickly' check only executes if isInvulnerableDimensionChange return false
    }

    @Redirect(method = "handleMovePlayer",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;isChangingDimension()Z", ordinal = 1))
    private boolean movementCheck$onMovedWronglyCheck(final ServerPlayerEntity player) {
        if (SpongeGameConfigs.getForWorld(this.player.level).get().movementChecks.movedWrongly) {
            return player.isChangingDimension();
        }
        return true; // The 'moved too quickly' check only executes if isInvulnerableDimensionChange return false
    }

    @ModifyConstant(method = "handleMoveVehicle", constant = @Constant(doubleValue = 100, ordinal = 0), slice =
        @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/vector/Vector3d;lengthSqr()D", ordinal = 1),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/ServerPlayNetHandler;isSingleplayerOwner()Z", ordinal = 0))
    )
    private double movementCheck$onVehicleMovedTooQuicklyCheck(final double val) {
        if (SpongeGameConfigs.getForWorld(this.player.level).get().movementChecks.player.vehicleMovedTooQuickly) {
            return val;
        }
        return Double.NaN; // The 'vehicle moved too quickly' check only executes if the squared difference of the motion vectors lengths is greater than 100
    }

    @ModifyConstant(method = "handleMoveVehicle",
        constant = @Constant(doubleValue = 0.0625D, ordinal = 0),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/entity/Entity;move(Lnet/minecraft/entity/MoverType;Lnet/minecraft/util/math/vector/Vector3d;)V",
                ordinal = 0),
            to  = @At(
                value = "INVOKE",
                target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",
                ordinal = 0,
                remap = false)
    ))
    private double movementCheck$onMovedWronglySecond(final double val) {
        if (SpongeGameConfigs.getForWorld(this.player.level).get().movementChecks.movedWrongly) {
            return val;
        }
        return Double.NaN; // The second 'moved wrongly' check only executes if the length of the movement vector is greater than 0.0625D
    }
}
