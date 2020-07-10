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
package org.spongepowered.vanilla.mixin.core.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.api.Engine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.launch.Launcher;
import org.spongepowered.vanilla.VanillaLifecycle;
import org.spongepowered.vanilla.launch.ClientLauncher;
import org.spongepowered.vanilla.launch.VanillaClient;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin_Vanilla {

    @Shadow @Final private Thread thread;
    private VanillaLifecycle vanilla$lifeCycle;

    @Inject(method = "run", at = @At("HEAD"))
    private void vanilla$prepareGameAndLoadPlugins(CallbackInfo ci) {
        final ClientLauncher launcher = Launcher.getInstance();
        launcher.setupInjection((VanillaClient) this);
        launcher.loadPlugins((VanillaClient) this);

        try {
            PhaseTracker.CLIENT.setThread(this.thread);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not initialize the client PhaseTracker!");
        }

        this.vanilla$lifeCycle = new VanillaLifecycle((Engine) this);
        this.vanilla$lifeCycle.establishFactories();
        this.vanilla$lifeCycle.initTimings();
        this.vanilla$lifeCycle.registerPluginListeners();
        this.vanilla$lifeCycle.callConstructEventToPlugins();
        this.vanilla$lifeCycle.establishServices();
    }
}
