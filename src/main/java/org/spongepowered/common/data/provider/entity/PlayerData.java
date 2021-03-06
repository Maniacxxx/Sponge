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
package org.spongepowered.common.data.provider.entity;

import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.HandSide;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandPreference;
import org.spongepowered.common.accessor.entity.player.PlayerAbilitiesAccessor;
import org.spongepowered.common.accessor.entity.player.PlayerEntityAccessor;
import org.spongepowered.common.accessor.util.FoodStatsAccessor;
import org.spongepowered.common.bridge.entity.player.PlayerEntityBridge;
import org.spongepowered.common.bridge.entity.player.ServerPlayerEntityBridge;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.util.ExperienceHolderUtil;

public final class PlayerData {

    private static final double EXHAUSTION_MAX = 40.0;
    private static final double SATURATION_MAX = 40.0;
    private static final int FOOD_LEVEL_MAX = 20;

    private PlayerData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(PlayerEntity.class)
                    .create(Keys.CAN_FLY)
                        .get(h -> h.abilities.mayfly)
                        .set((h, v) -> {
                            h.abilities.mayfly = v;
                            h.onUpdateAbilities();
                        })
                    .create(Keys.DOMINANT_HAND)
                        .get(h -> (HandPreference) (Object) h.getMainArm())
                        .set((h, v) -> h.setMainArm((HandSide) (Object) v))
                    .create(Keys.EXHAUSTION)
                        .get(h -> (double) ((FoodStatsAccessor) h.getFoodData()).accessor$exhaustionLevel())
                        .setAnd((h, v) -> {
                            if (v < 0 || v > PlayerData.EXHAUSTION_MAX) {
                                return false;
                            }
                            ((FoodStatsAccessor) h.getFoodData()).accessor$exhaustionLevel(v.floatValue());
                            return true;
                        })
                    .create(Keys.EXPERIENCE)
                        .get(h -> h.totalExperience)
                        .set(ExperienceHolderUtil::setExperience)
                        .delete(h -> ExperienceHolderUtil.setExperience(h, 0))
                    .create(Keys.EXPERIENCE_FROM_START_OF_LEVEL)
                        .get(PlayerEntity::getXpNeededForNextLevel)
                    .create(Keys.EXPERIENCE_LEVEL)
                        .get(h -> h.experienceLevel)
                        .setAnd((h, v) -> {
                            if (v < 0) {
                                return false;
                            }
                            h.totalExperience = ExperienceHolderUtil.xpAtLevel(v);
                            h.experienceProgress = 0;
                            h.experienceLevel = v;
                            ((ServerPlayerEntityBridge) h).bridge$refreshExp();
                            return true;
                        })
                    .create(Keys.EXPERIENCE_SINCE_LEVEL)
                        .get(h -> ((PlayerEntityBridge) h).bridge$getExperienceSinceLevel())
                        .setAnd((h, v) -> {
                            if (v < 0) {
                                return false;
                            }
                            ExperienceHolderUtil.setExperienceSinceLevel(h, v);
                            return true;
                        })
                        .delete(h -> ExperienceHolderUtil.setExperience(h, 0))
                    .create(Keys.FLYING_SPEED)
                        .get(h -> (double) h.abilities.getFlyingSpeed())
                        .setAnd((h, v) -> {
                            if (v < 0) {
                                return false;
                            }
                            ((PlayerAbilitiesAccessor) h.abilities).accessor$flyingSpeed(v.floatValue());
                            h.onUpdateAbilities();
                            return true;
                        })
                    .create(Keys.FOOD_LEVEL)
                        .get(h -> h.getFoodData().getFoodLevel())
                        .setAnd((h, v) -> {
                            if (v < 0 || v > PlayerData.FOOD_LEVEL_MAX) {
                                return false;
                            }
                            h.getFoodData().setFoodLevel(v);
                            return true;
                        })
                    .create(Keys.IS_FLYING)
                        .get(h -> h.abilities.flying)
                        .set((h, v) -> {
                            h.abilities.flying = v;
                            h.onUpdateAbilities();
                        })
                    .create(Keys.IS_SLEEPING)
                        .get(PlayerEntity::isSleeping)
                    .create(Keys.IS_SLEEPING_IGNORED)
                        .get(PlayerEntity::isSleeping)
                    .create(Keys.MAX_EXHAUSTION)
                        .get(h -> PlayerData.EXHAUSTION_MAX)
                    .create(Keys.MAX_FOOD_LEVEL)
                        .get(h -> PlayerData.FOOD_LEVEL_MAX)
                    .create(Keys.MAX_SATURATION)
                        .get(h -> PlayerData.SATURATION_MAX)
                    .create(Keys.SATURATION)
                        .get(h -> (double) h.getFoodData().getSaturationLevel())
                        .setAnd((h, v) -> {
                            if (v < 0 || v > PlayerData.SATURATION_MAX) {
                                return false;
                            }
                            ((FoodStatsAccessor) h.getFoodData()).accessor$saturationLevel(v.floatValue());
                            return true;
                        })
                    .create(Keys.SLEEP_TIMER)
                        .get(PlayerEntity::getSleepTimer)
                        .set((p, i) -> ((PlayerEntityAccessor) p).accessor$sleepCounter(i))
                    .create(Keys.WALKING_SPEED)
                        .get(h -> (double) h.abilities.getWalkingSpeed())
                        .set((h, v) -> {
                            ((PlayerAbilitiesAccessor) h.abilities).accessor$walkingSpeed(v.floatValue());
                            h.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(v);
                            h.onUpdateAbilities();
                        })
                .asMutable(PlayerEntityBridge.class)
                    .create(Keys.AFFECTS_SPAWNING)
                        .get(PlayerEntityBridge::bridge$affectsSpawning)
                        .set(PlayerEntityBridge::bridge$setAffectsSpawning);
    }
    // @formatter:on
}
