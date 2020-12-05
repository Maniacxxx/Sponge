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
package org.spongepowered.common.mixin.api.mcp.world.server;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.ticket.Ticket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.accessor.world.server.TicketAccessor;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.SpongeTicks;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3i;

import java.util.Optional;

@Mixin(TicketManager.class)
public abstract class TicketManagerMixin_API implements org.spongepowered.api.world.server.ticket.TicketManager {

    @Shadow private void shadow$register(final long chunkpos, final net.minecraft.world.server.Ticket<?> ticket) { };
    @Shadow protected abstract void shadow$release(long chunkPosIn, net.minecraft.world.server.Ticket<?> ticketIn);
    @Shadow @Final private Long2ObjectOpenHashMap<SortedArraySet<net.minecraft.world.server.Ticket<?>>> tickets;
    @Shadow private long currentTime;

    @Override
    @SuppressWarnings({ "ConstantConditions", "unchecked" })
    public boolean isValid(final Ticket ticket) {
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
    public Ticks getDefaultLifetime() {
        return new SpongeTicks(TicketType.FORCED.getLifespan());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Ticks getTimeLeft(final Ticket ticket) {
        if (this.isValid(ticket)) {
            return new SpongeTicks(((TicketAccessor<ChunkPos>) ticket).accessor$getTimestamp() - this.currentTime);
        }
        return Ticks.zero();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean renew(final Ticket ticket) {
        if (this.isValid(ticket)) {
            ((TicketAccessor<ChunkPos>) ticket).accessor$setTimestamp(this.currentTime + TicketType.FORCED.getLifespan());
            return true;
        }
        return false;
    }

    @Override
    public Optional<Ticket> request(final Vector3i chunkPos) {
        final ChunkPos cp = VecHelper.toChunkPos(chunkPos);
        final net.minecraft.world.server.Ticket<?> ticketToRequest =
                // TODO: variable view distances?
                TicketAccessor.accessor$createInstance(TicketType.FORCED,
                        Constants.ChunkTicket.MAXIMUM_CHUNK_TICKET_LEVEL - Constants.ChunkTicket.DEFAULT_CHUNK_TICKET_DISTANCE, cp);
        this.shadow$register(cp.asLong(), ticketToRequest);
        return Optional.of((Ticket) (Object) ticketToRequest);
    }

    @Override
    @SuppressWarnings({"ConstantConditions", "unchecked"})
    public boolean release(final Ticket ticket) {
        if (this.isValid(ticket)) {
            final TicketAccessor<ChunkPos> ticketAccessor = ((TicketAccessor<ChunkPos>) ticket);
            final ChunkPos chunkPos = ticketAccessor.accessor$getValue();
            this.shadow$release(chunkPos.asLong(), (net.minecraft.world.server.Ticket<?>) (Object) ticket);
            return true;
        }
        return false;
    }

}
