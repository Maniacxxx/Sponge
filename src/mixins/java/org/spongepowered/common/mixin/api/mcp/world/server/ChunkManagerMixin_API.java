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

import net.minecraft.world.server.ChunkManager;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.Ticket;
import org.spongepowered.api.world.server.TicketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.world.server.ChunkManagerBridge;
import org.spongepowered.common.world.server.SpongeTicketType;
import org.spongepowered.math.vector.Vector3i;

import java.util.Collection;
import java.util.Optional;

@Mixin(ChunkManager.class)
public abstract class ChunkManagerMixin_API implements org.spongepowered.api.world.server.ChunkManager {

    @Shadow @Final private net.minecraft.world.server.ServerWorld world;

    @Override
    public ServerWorld getWorld() {
        return (ServerWorld) this.world;
    }

    @Override
    public boolean isValid(final Ticket<?> ticket) {
        return ((ChunkManagerBridge) this).bridge$getTicketManager().bridge$checkTicketValid(ticket);
    }

    @Override
    public Ticks getTimeLeft(final Ticket<?> ticket) {
        return ((ChunkManagerBridge) this).bridge$getTicketManager().bridge$getTimeLeft(ticket);
    }

    @Override
    public <T> Optional<Ticket<T>> requestTicket(final TicketType<T> type, final Vector3i chunkPosition, final T value, final int radius) {
        if (radius < 1) {
            throw new IllegalArgumentException("The radius must be positive.");
        }
        return ((ChunkManagerBridge) this).bridge$getTicketManager()
                .bridge$registerTicket(this.getWorld(), (SpongeTicketType<?, T>) type, chunkPosition, value, radius);
    }

    @Override
    public boolean renewTicket(final Ticket<?> ticket) {
        return ((ChunkManagerBridge) this).bridge$getTicketManager().bridge$renewTicket(ticket);
    }

    @Override
    public boolean releaseTicket(final Ticket<?> ticket) {
        return ((ChunkManagerBridge) this).bridge$getTicketManager().bridge$releaseTicket(ticket);
    }

    @Override
    public <T> Collection<Ticket<T>> getTickets(final TicketType<T> type) {
        return ((ChunkManagerBridge) this).bridge$getTicketManager().bridge$getTickets((SpongeTicketType<?, T>) type);
    }

}
