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
package org.spongepowered.common.mixin.core.world.server;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.Ticket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.accessor.world.server.TicketAccessor;
import org.spongepowered.common.bridge.world.TicketManagerBridge;
import org.spongepowered.common.bridge.world.server.TicketBridge;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.SpongeTicks;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.server.SpongeTicketType;
import org.spongepowered.math.vector.Vector3i;

import java.util.Optional;

@Mixin(TicketManager.class)
public abstract class TicketManagerMixin implements TicketManagerBridge {

    @Shadow private void shadow$register(final long chunkpos, final net.minecraft.world.server.Ticket<?> ticket) { }
    @Shadow protected abstract void shadow$release(long chunkPosIn, net.minecraft.world.server.Ticket<?> ticketIn);
    @Shadow @Final private Long2ObjectOpenHashMap<SortedArraySet<net.minecraft.world.server.Ticket<?>>> tickets;
    @Shadow private long currentTime;

    @Shadow protected abstract void tick();

    @Override
    @SuppressWarnings({ "ConstantConditions", "unchecked" })
    public boolean bridge$checkTicketValid(final Ticket<?> ticket) {
        // is the ticket in our manager?
        final net.minecraft.world.server.Ticket<?> nativeTicket = ((net.minecraft.world.server.Ticket<?>) (Object) ticket);
        if (nativeTicket.getType() == TicketType.FORCED) {
            final TicketAccessor<ChunkPos> ticketAccessor = ((TicketAccessor<ChunkPos>) ticket);
            final ChunkPos chunkPos = ticketAccessor.accessor$getValue();
            if (this.tickets.computeIfAbsent(chunkPos.asLong(), x -> SortedArraySet.newSet(4)).contains(nativeTicket)) {
                // check to see if it's expired.
                return ticketAccessor.accessor$isExpired(this.currentTime);
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Ticks bridge$getTimeLeft(final Ticket<?> ticket) {
        if (this.bridge$checkTicketValid(ticket)) {
            return new SpongeTicks(((TicketAccessor<ChunkPos>) ticket).accessor$getTimestamp() - this.currentTime);
        }
        return Ticks.zero();
    }

    @Override
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public boolean bridge$renewTicket(final Ticket<?> ticket) {
        if (this.bridge$checkTicketValid(ticket)) {
            final net.minecraft.world.server.Ticket<?> nativeTicket = (net.minecraft.world.server.Ticket<?>) (Object) ticket;
            ((TicketAccessor<ChunkPos>) ticket).accessor$setTimestamp(this.currentTime + nativeTicket.getType().getLifespan());
            return true;
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S, T> Optional<Ticket<T>> bridge$registerTicket(final SpongeTicketType<S, T> ticketType, final Vector3i pos, final T value, final int distanceLimit) {
        final TicketType<S> type = ticketType.getWrappedType();
        final net.minecraft.world.server.Ticket<?> ticketToRequest =
                TicketAccessor.accessor$createInstance(
                        type,
                        Constants.ChunkTicket.MAXIMUM_CHUNK_TICKET_LEVEL - distanceLimit,
                        ticketType.getNativeType(value));
        this.shadow$register(VecHelper.toChunkPos(pos).asLong(), ticketToRequest);
        return Optional.of((Ticket<T>) (Object) ticketToRequest);
    }

    @Override
    @SuppressWarnings({"ConstantConditions"})
    public boolean bridge$releaseTicket(final Ticket<?> ticket) {
        if (this.bridge$checkTicketValid(ticket)) {
            this.shadow$release(((TicketBridge) ticket).bridge$getChunkPosition(),
                    (net.minecraft.world.server.Ticket<?>) (Object) ticket);
            return true;
        }
        return false;
    }

    @Inject(method = "register(JLnet/minecraft/world/server/Ticket;)V", at = @At("HEAD"))
    private void impl$addChunkPosToTicket(final long chunkPos, final Ticket<?> ticket, final CallbackInfo ci) {
        ((TicketBridge) ticket).bridge$setChunkPosition(chunkPos);
    }

}
