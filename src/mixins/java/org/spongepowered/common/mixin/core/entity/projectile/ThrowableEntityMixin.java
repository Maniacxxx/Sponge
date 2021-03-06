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
package org.spongepowered.common.mixin.core.entity.projectile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.tileentity.EndGatewayTileEntity;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.projectile.source.ProjectileSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.bridge.world.WorldBridge;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.PhaseTracker;

@Mixin(ThrowableEntity.class)
public abstract class ThrowableEntityMixin extends ProjectileEntityMixin {

    @Redirect(method = "tick()V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/projectile/ThrowableEntity;onHit(Lnet/minecraft/util/math/RayTraceResult;)V"
        )
    )
    private void impl$handleProjectileImpact(final ThrowableEntity projectile, final RayTraceResult movingObjectPosition) {
        if (((WorldBridge) this.level).bridge$isFake() || movingObjectPosition.getType() == RayTraceResult.Type.MISS) {
            this.shadow$onHit(movingObjectPosition);
            return;
        }

        if (SpongeCommonEventFactory.handleCollideImpactEvent(projectile, (ProjectileSource) this.shadow$getOwner(), movingObjectPosition)) {
            this.shadow$remove();
        } else {
            this.shadow$onHit(movingObjectPosition);
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/EndGatewayTileEntity;teleportEntity(Lnet/minecraft/entity/Entity;)V"))
    private void impl$createCauseFrameForGatewayTeleport(EndGatewayTileEntity endGatewayTileEntity, Entity entityIn) {
        if (this.shadow$getCommandSenderWorld().isClientSide) {
            return;
        }

        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(endGatewayTileEntity);
            frame.pushCause(entityIn);
            endGatewayTileEntity.teleportEntity(entityIn);
        }
    }

}
