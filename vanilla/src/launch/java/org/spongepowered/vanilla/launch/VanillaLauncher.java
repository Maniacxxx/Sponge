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
package org.spongepowered.vanilla.launch;

import org.spongepowered.common.launch.Launcher;
import org.spongepowered.common.launch.plugin.DummyPluginContainer;
import org.spongepowered.plugin.metadata.PluginMetadata;
import org.spongepowered.plugin.metadata.util.PluginMetadataHelper;
import org.spongepowered.vanilla.launch.plugin.VanillaPluginManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

public abstract class VanillaLauncher extends Launcher {

    // TODO Minecraft 1.14 - DI
    protected VanillaLauncher() {
        super(new VanillaPluginManager());
    }

    @Override
    protected void createPlatformPlugins(final Path gameDirectory) {
        try {
            final Collection<PluginMetadata> read = PluginMetadataHelper.builder().build().read(VanillaLauncher.class.getResourceAsStream("/plugins.json"));
            for (final PluginMetadata metadata : read) {
                this.getPluginManager().addPlugin(new DummyPluginContainer(metadata, gameDirectory, this.getLogger(), this));
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load metadata information for the implementation! This should be impossible!");
        }
    }
}
