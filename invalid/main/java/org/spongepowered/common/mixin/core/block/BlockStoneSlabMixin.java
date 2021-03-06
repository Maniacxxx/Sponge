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
package org.spongepowered.common.mixin.invalid.core.block;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockStoneSlab;
import net.minecraft.block.BlockStoneSlabNew;
import net.minecraft.block.SlabBlock;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.DataManipulator.Immutable;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutablePortionData;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableSeamlessData;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableSlabData;
import org.spongepowered.api.data.type.SlabType;
import org.spongepowered.api.data.type.SlabTypes;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.data.manipulator.immutable.block.ImmutableSpongePortionData;
import org.spongepowered.common.data.manipulator.immutable.block.ImmutableSpongeSlabData;
import org.spongepowered.common.mixin.core.block.BlockMixin;

import java.util.Optional;

@Mixin(value = {BlockStoneSlabNew.class, BlockStoneSlab.class})
public abstract class BlockStoneSlabMixin extends BlockMixin {

    @SuppressWarnings("RedundantTypeArguments") // some JDK's can fail to compile without the explicit type generics
    @Override
    public ImmutableList<Immutable<?, ?>> bridge$getManipulators(final net.minecraft.block.BlockState blockState) {
        return ImmutableList.<Immutable<?, ?>>of(this.impl$getSlabTypeFor(blockState), this.impl$getPortionTypeFor(blockState));
    }

    @Override
    public boolean bridge$supports(final Class<? extends Immutable<?, ?>> immutable) {
        return ImmutableSlabData.class.isAssignableFrom(immutable) || ImmutablePortionData.class.isAssignableFrom(immutable);
    }

    @Override
    public Optional<BlockState> bridge$getStateWithData(final net.minecraft.block.BlockState blockState, final Immutable<?, ?> manipulator) {
        if (manipulator instanceof ImmutableSlabData) {
            final SlabType type = ((ImmutableSlabData) manipulator).type().get();
            if (blockState.getBlock() instanceof BlockStoneSlab) {
                if (!type.equals(SlabTypes.RED_SAND)) {
                    final BlockStoneSlab.EnumType slabType = (BlockStoneSlab.EnumType) (Object) type;
                    return Optional.of((BlockState) blockState.withProperty(BlockStoneSlab.VARIANT, slabType));
                }
            } else if (blockState.getBlock() instanceof BlockStoneSlabNew) {
                if (type.equals(SlabTypes.RED_SAND)) {
                    final BlockStoneSlabNew.EnumType slabType = (BlockStoneSlabNew.EnumType) (Object) type;
                    return Optional.of((BlockState) blockState.withProperty(BlockStoneSlabNew.VARIANT, slabType));
                }
            }
            return Optional.empty();
        } else if (manipulator instanceof ImmutablePortionData) {
            final PortionType portionType = ((ImmutablePortionData) manipulator).type().get();
            return Optional.of((BlockState) blockState.withProperty(SlabBlock.HALF, (SlabBlock.EnumBlockHalf) (Object) portionType));
        }
        if (manipulator instanceof ImmutableSeamlessData) {
            final boolean seamless = ((ImmutableSeamlessData) manipulator).seamless().get();
            if (blockState.getBlock() instanceof BlockStoneSlab) {
                return Optional.of((BlockState) blockState.withProperty(BlockStoneSlab.SEAMLESS, seamless));
            }
            if (blockState.getBlock() instanceof BlockStoneSlabNew) {
                return Optional.of((BlockState) blockState.withProperty(BlockStoneSlabNew.SEAMLESS, seamless));
            }
        }
        return super.bridge$getStateWithData(blockState, manipulator);
    }

    @Override
    public <E> Optional<BlockState> bridge$getStateWithValue(final net.minecraft.block.BlockState blockState, final Key<? extends Value<E>> key, final E value) {
        if (key.equals(Keys.SLAB_TYPE)) {
            final SlabType type = (SlabType) value;
            if (blockState.getBlock() instanceof BlockStoneSlab) {
                if (!type.equals(SlabTypes.RED_SAND)) {
                    final BlockStoneSlab.EnumType slabType = (BlockStoneSlab.EnumType) value;
                    return Optional.of((BlockState) blockState.withProperty(BlockStoneSlab.VARIANT, slabType));
                }
            } else if (blockState.getBlock() instanceof BlockStoneSlabNew) {
                if (type.equals(SlabTypes.RED_SAND)) {
                    final BlockStoneSlabNew.EnumType slabType = (BlockStoneSlabNew.EnumType) value;
                    return Optional.of((BlockState) blockState.withProperty(BlockStoneSlabNew.VARIANT, slabType));
                }
            }
            return Optional.empty();
        } else if (key.equals(Keys.PORTION_TYPE)) {
            return Optional.of((BlockState) blockState.withProperty(SlabBlock.HALF, (SlabBlock.EnumBlockHalf) value));
        }
        if (key.equals(Keys.SEAMLESS)) {
            final boolean seamless = (Boolean) value;
            if (blockState.getBlock() instanceof BlockStoneSlab) {
                return Optional.of((BlockState) blockState.withProperty(BlockStoneSlab.SEAMLESS, seamless));
            }
            if (blockState.getBlock() instanceof BlockStoneSlabNew) {
                return Optional.of((BlockState) blockState.withProperty(BlockStoneSlabNew.SEAMLESS, seamless));
            }
        }
        return super.bridge$getStateWithValue(blockState, key, value);
    }

    @SuppressWarnings("ConstantConditions")
    private ImmutableSlabData impl$getSlabTypeFor(final net.minecraft.block.BlockState blockState) {
        return ImmutableDataCachingUtil.getManipulator(ImmutableSpongeSlabData.class,
                blockState.getBlock() instanceof BlockStoneSlab
                        ? (SlabType) (Object) blockState.get(BlockStoneSlab.VARIANT)
                        : blockState.getBlock() instanceof BlockStoneSlabNew
                                ? (SlabType) (Object) blockState.get(BlockStoneSlabNew.VARIANT)
                                : SlabTypes.COBBLESTONE);
    }

    private ImmutablePortionData impl$getPortionTypeFor(final net.minecraft.block.BlockState blockState) {
        return ImmutableDataCachingUtil.getManipulator(ImmutableSpongePortionData.class, blockState.get(SlabBlock.HALF));
    }
}
