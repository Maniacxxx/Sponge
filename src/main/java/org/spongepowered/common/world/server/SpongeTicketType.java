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
package org.spongepowered.common.world.server;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.TicketType;
import org.spongepowered.common.accessor.world.server.TicketTypeAccessor;
import org.spongepowered.common.util.SpongeTicks;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3i;

import java.util.Locale;
import java.util.function.Function;

public final class SpongeTicketType<S, T> implements TicketType<T> {

    private final net.minecraft.world.server.TicketType<S> wrappedType;
    private final Function<T, S> spongeToNative;
    private final Function<S, T> nativeToSponge;
    private final ResourceKey key;
    private final Ticks lifetime;

    public static SpongeTicketType<ChunkPos, Vector3i> createForChunkPos(final net.minecraft.world.server.TicketType<ChunkPos> ticketType) {
        return new SpongeTicketType<>(ticketType, VecHelper::toChunkPos, VecHelper::toVec3i);
    }

    public static SpongeTicketType<BlockPos, Vector3i> createForBlockPos(final net.minecraft.world.server.TicketType<BlockPos> ticketType) {
        return new SpongeTicketType<>(ticketType, VecHelper::toBlockPos, VecHelper::toVector3i);
    }

    @SuppressWarnings("unchecked")
    public static <S, T> SpongeTicketType<S, T> createForCastedType(final net.minecraft.world.server.TicketType<S> ticketType) {
        return new SpongeTicketType<>(ticketType, tIn -> (S) tIn, sIn -> (T) sIn);
    }

    public SpongeTicketType(final net.minecraft.world.server.TicketType<S> wrappedType, final Function<T, S> spongeToNative,
            final Function<S, T> nativeToSponge) {
        this.wrappedType = wrappedType;
        this.spongeToNative = spongeToNative;
        this.nativeToSponge = nativeToSponge;
        this.key = ResourceKey.sponge(((TicketTypeAccessor) wrappedType).accessor$getName().toLowerCase(Locale.ROOT));
        this.lifetime = new SpongeTicks(this.wrappedType.getLifespan());
    }

    @Override
    public ResourceKey getKey() {
        return this.key;
    }

    public net.minecraft.world.server.TicketType<S> getWrappedType() {
        return this.wrappedType;
    }

    public S getNativeType(final T spongeType) {
        return this.spongeToNative.apply(spongeType);
    }

    public T getSpongeType(final S nativeType) {
        return this.nativeToSponge.apply(nativeType);
    }

    @Override
    public Ticks getLifetime() {
        return this.lifetime;
    }

}
