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
package org.spongepowered.common.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SChangeGameStatePacket;
import net.minecraft.network.play.server.SPlayEntityEffectPacket;
import net.minecraft.network.play.server.SPlaySoundEventPacket;
import net.minecraft.network.play.server.SPlayerAbilitiesPacket;
import net.minecraft.network.play.server.SServerDifficultyPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IWorldInfo;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.portal.PortalType;
import org.spongepowered.common.accessor.entity.LivingEntityAccessor;
import org.spongepowered.common.accessor.entity.player.ServerPlayerEntityAccessor;
import org.spongepowered.common.bridge.CreatorTrackedBridge;
import org.spongepowered.common.bridge.data.VanishableBridge;
import org.spongepowered.common.bridge.entity.PlatformEntityBridge;
import org.spongepowered.common.bridge.entity.player.ServerPlayerEntityBridge;
import org.spongepowered.common.bridge.world.PlatformServerWorldBridge;
import org.spongepowered.common.bridge.world.ServerWorldBridge;
import org.spongepowered.common.bridge.world.WorldBridge;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.hooks.PlatformHooks;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.DimensionChangeResult;
import org.spongepowered.common.world.portal.PortalHelper;
import org.spongepowered.common.world.portal.WrappedITeleporterPortalType;
import org.spongepowered.math.vector.Vector3d;

import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class EntityUtil {

    public static final Function<PhaseContext<?>, Supplier<Optional<User>>> ENTITY_CREATOR_FUNCTION = (context) ->
        () -> Stream.<Supplier<Optional<User>>>builder()
            .add(() -> context.getSource(User.class))
            .add(context::getNotifier)
            .add(context::getCreator)
            .build()
            .map(Supplier::get)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();

    private EntityUtil() {
    }

    public static void performPostChangePlayerWorldLogic(final ServerPlayerEntity player, final ServerWorld fromWorld,
            final ServerWorld originalToWorld, final ServerWorld toWorld, final boolean isPortal) {
        // Sponge Start - Send any platform dimension data
        ((ServerPlayerEntityBridge) player).bridge$sendDimensionData(player.connection.connection, toWorld.dimensionType(), toWorld.dimension());
        // Sponge End
        IWorldInfo worldinfo = toWorld.getLevelData();
        // We send dimension change for portals before loading chunks
        if (!isPortal) {
            // Sponge Start - Allow the platform to handle how dimension changes are sent down
            ((ServerPlayerEntityBridge) player).bridge$sendChangeDimension(toWorld.dimensionType(), toWorld.dimension(), BiomeManager.obfuscateSeed(toWorld.getSeed()),
                    player.gameMode.getGameModeForPlayer(), player.gameMode.getPreviousGameModeForPlayer(),
                    toWorld.isDebug(), toWorld.isFlat(), true);
        }
        // Sponge End
        player.connection.send(new SServerDifficultyPacket(worldinfo.getDifficulty(), worldinfo.isDifficultyLocked()));
        final PlayerList playerlist = player.getServer().getPlayerList();
        playerlist.sendPlayerPermissionLevel(player);

        // Sponge Start - Have the platform handle removing the entity from the world. Move this to after the event call so
        //                that we do not remove the player from the world unless we really have teleported..
        ((PlatformServerWorldBridge) fromWorld).bridge$removeEntity(player, true);
        ((PlatformEntityBridge) player).bridge$revive();
        // Sponge End

        player.setLevel(toWorld);
        toWorld.addDuringPortalTeleport(player);
        if (isPortal) {
            ((ServerPlayerEntityAccessor) player).invoker$triggerDimensionChangeTriggers(toWorld);
        }
        player.gameMode.setLevel(toWorld);
        player.connection.send(new SPlayerAbilitiesPacket(player.abilities));
        playerlist.sendLevelInfo(player, toWorld);
        playerlist.sendAllPlayerInfo(player);

        for (final EffectInstance effectinstance : player.getActiveEffects()) {
            player.connection.send(new SPlayEntityEffectPacket(player.getId(), effectinstance));
        }

        if (isPortal) {
            player.connection.send(new SPlaySoundEventPacket(1032, BlockPos.ZERO, 0, false));
        }

        ((ServerWorldBridge) fromWorld).bridge$getBossBarManager().onPlayerDisconnect(player);
        ((ServerWorldBridge) toWorld).bridge$getBossBarManager().onPlayerDisconnect(player);

        ((ServerPlayerEntityAccessor) player).accessor$lastSentExp(-1);
        ((ServerPlayerEntityAccessor) player).accessor$lastSentHealth(-1.0f);
        ((ServerPlayerEntityAccessor) player).accessor$lastSentFood(-1);

        if (!isPortal) {
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.yRot, player.xRot);
            player.connection.resetPosition();
        }

        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }

        // Sponge Start - Call platform event hook after changing dimensions
        PlatformHooks.INSTANCE.getEventHooks().callChangeEntityWorldEventPost(player, fromWorld, originalToWorld);
        // Sponge End
    }

    public static boolean isEntityDead(final net.minecraft.entity.Entity entity) {
        if (entity instanceof LivingEntity) {
            final LivingEntity base = (LivingEntity) entity;
            return base.getHealth() <= 0 || base.deathTime > 0 || ((LivingEntityAccessor) entity).accessor$dead();
        }
        return entity.removed;
    }

    public static boolean processEntitySpawnsFromEvent(final SpawnEntityEvent event, final Supplier<Optional<User>> entityCreatorSupplier) {
        boolean spawnedAny = false;
        for (final org.spongepowered.api.entity.Entity entity : event.getEntities()) {
            // Here is where we need to handle the custom items potentially having custom entities
            spawnedAny = EntityUtil.processEntitySpawn(entity, entityCreatorSupplier);
        }
        return spawnedAny;
    }

    public static boolean processEntitySpawnsFromEvent(final PhaseContext<?> context, final SpawnEntityEvent destruct) {
        return EntityUtil.processEntitySpawnsFromEvent(destruct, EntityUtil.ENTITY_CREATOR_FUNCTION.apply(context));
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean processEntitySpawn(final org.spongepowered.api.entity.Entity entity, final Supplier<Optional<User>> supplier) {
        final Entity minecraftEntity = (Entity) entity;
        if (minecraftEntity instanceof ItemEntity) {
            final ItemStack item = ((ItemEntity) minecraftEntity).getItem();
            if (!item.isEmpty()) {
                final Optional<Entity> customEntityItem = Optional.ofNullable(PlatformHooks.INSTANCE.getWorldHooks().getCustomEntityIfItem(minecraftEntity));
                if (customEntityItem.isPresent()) {
                    // Bypass spawning the entity item, since it is established that the custom entity is spawned.
                    final Entity entityToSpawn = customEntityItem.get();
                    supplier.get()
                        .ifPresent(spawned -> {
                            if (entityToSpawn instanceof CreatorTrackedBridge) {
                                ((CreatorTrackedBridge) entityToSpawn).tracked$setCreatorReference(spawned);
                            }
                        });
                    if (entityToSpawn.removed) {
                        entityToSpawn.removed = false;
                    }
                    // Since forge already has a new event thrown for the entity, we don't need to throw
                    // the event anymore as sponge plugins getting the event after forge mods will
                    // have the modified entity list for entities, so no need to re-capture the entities.
                    entityToSpawn.level.addFreshEntity(entityToSpawn);
                    return true;
                }
            }
        }

        supplier.get()
            .ifPresent(spawned -> {
                if (entity instanceof CreatorTrackedBridge) {
                    ((CreatorTrackedBridge) entity).tracked$setCreatorReference(spawned);
                }
            });
        // Allowed to call force spawn directly since we've applied creator and custom item logic already
        ((net.minecraft.world.World) entity.getWorld()).addFreshEntity((Entity) entity);
        return true;
    }

    /**
     * This is used to create the "dropping" motion for items caused by players. This
     * specifically was being used (and should be the correct math) to drop from the
     * player, when we do item stack captures preventing entity items being created.
     *
     * @param dropAround True if it's being "dropped around the player like dying"
     * @param player The player to drop around from
     * @param random The random instance
     * @return The motion vector
     */
    @SuppressWarnings("unused")
    private static Vector3d createDropMotion(final boolean dropAround, final PlayerEntity player, final Random random) {
        double x;
        double y;
        double z;
        if (dropAround) {
            final float f = random.nextFloat() * 0.5F;
            final float f1 = random.nextFloat() * ((float) Math.PI * 2F);
            x = -MathHelper.sin(f1) * f;
            z = MathHelper.cos(f1) * f;
            y = 0.20000000298023224D;
        } else {
            float f2 = 0.3F;
            x = -MathHelper.sin(player.yRot * 0.017453292F) * MathHelper.cos(player.xRot * 0.017453292F) * f2;
            z = MathHelper.cos(player.yRot * 0.017453292F) * MathHelper.cos(player.xRot * 0.017453292F) * f2;
            y = - MathHelper.sin(player.xRot * 0.017453292F) * f2 + 0.1F;
            final float f3 = random.nextFloat() * ((float) Math.PI * 2F);
            f2 = 0.02F * random.nextFloat();
            x += Math.cos(f3) * f2;
            y += (random.nextFloat() - random.nextFloat()) * 0.1F;
            z += Math.sin(f3) * f2;
        }
        return new Vector3d(x, y, z);
    }


    public static boolean isUntargetable(Entity from, Entity target) {
        if (((VanishableBridge) target).bridge$isVanished() && ((VanishableBridge) target).bridge$isUntargetable()) {
            return true;
        }
        // Temporary fix for https://bugs.mojang.com/browse/MC-149563
        return from.level != target.level;
    }
}
