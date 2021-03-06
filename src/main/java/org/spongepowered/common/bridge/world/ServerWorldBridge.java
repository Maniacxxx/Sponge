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
package org.spongepowered.common.bridge.world;

import net.minecraft.entity.Entity;
import net.minecraft.server.CustomServerBossInfoManager;
import net.minecraft.world.storage.SaveFormat;
import org.spongepowered.api.registry.RegistryHolder;
import org.spongepowered.api.world.explosion.Explosion;
import org.spongepowered.api.world.weather.Weather;
import org.spongepowered.common.relocate.co.aikar.timings.WorldTimingsHandler;
import org.spongepowered.math.vector.Vector3d;

public interface ServerWorldBridge {

    SaveFormat.LevelSave bridge$getLevelSave();

    boolean bridge$isLoaded();

    CustomServerBossInfoManager bridge$getBossBarManager();

    void bridge$setPreviousWeather(Weather weather);

    void bridge$updateRotation(Entity entityIn);

    void bridge$addEntityRotationUpdate(Entity entity, Vector3d rotation);

    WorldTimingsHandler bridge$getTimingsHandler();

    long bridge$getChunkUnloadDelay();

    void bridge$incrementChunkLoadCount();

    void bridge$updateConfigCache();

    long bridge$getWeatherStartTime();

    void bridge$setWeatherStartTime(long start);

    void bridge$setWeather(Weather weather, long ticks);

    long bridge$getDurationInTicks();

    void bridge$triggerExplosion(Explosion explosion);

    void bridge$setManualSave(boolean state);

    RegistryHolder bridge$registries();
}
