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
package org.spongepowered.common.mixin.core.server;

import com.google.inject.Injector;
import net.minecraft.command.CommandSource;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.concurrent.RecursiveEventLoop;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.FolderName;
import net.minecraft.world.storage.IServerConfiguration;
import net.minecraft.world.storage.SaveFormat;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.resourcepack.ResourcePack;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectProxy;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.SpongeServer;
import org.spongepowered.common.adventure.NativeComponentRenderer;
import org.spongepowered.common.applaunch.config.core.SpongeConfigs;
import org.spongepowered.common.bridge.command.CommandSourceProviderBridge;
import org.spongepowered.common.bridge.command.ICommandSourceBridge;
import org.spongepowered.common.bridge.server.MinecraftServerBridge;
import org.spongepowered.common.bridge.server.management.PlayerProfileCacheBridge;
import org.spongepowered.common.bridge.world.ServerWorldBridge;
import org.spongepowered.common.bridge.world.storage.IServerWorldInfoBridge;
import org.spongepowered.common.config.inheritable.InheritableConfigHandle;
import org.spongepowered.common.config.inheritable.WorldConfig;
import org.spongepowered.common.datapack.SpongeDataPackManager;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.item.recipe.ingredient.ResultUtil;
import org.spongepowered.common.item.recipe.ingredient.SpongeIngredient;
import org.spongepowered.common.relocate.co.aikar.timings.TimingsManager;
import org.spongepowered.common.resourcepack.SpongeResourcePack;
import org.spongepowered.common.service.server.SpongeServerScopedServiceProvider;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import javax.annotation.Nullable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin extends RecursiveEventLoop<TickDelayedTask> implements SpongeServer, MinecraftServerBridge,
        CommandSourceProviderBridge, SubjectProxy, ICommandSourceBridge {

    // @formatter:off
    @Shadow @Final private PlayerProfileCache profileCache;
    @Shadow @Final private static Logger LOGGER;
    @Shadow private int tickCount;
    @Shadow @Final protected IServerConfiguration worldData;
    @Shadow @Final protected SaveFormat.LevelSave storageSource;
    @Shadow @Final protected DynamicRegistries.Impl registryHolder;

    @Shadow public abstract CommandSource shadow$createCommandSourceStack();
    @Shadow public abstract Iterable<ServerWorld> shadow$getAllLevels();
    @Shadow public abstract boolean shadow$isDedicatedServer();
    @Shadow public abstract boolean shadow$isRunning();
    @Shadow public abstract PlayerList shadow$getPlayerList();
    @Shadow public abstract ResourcePackList shadow$getPackRepository();
    // @formatter:on

    @Shadow @Final private Map<RegistryKey<World>, ServerWorld> levels;
    @Nullable private SpongeServerScopedServiceProvider impl$serviceProvider;
    @Nullable private ResourcePack impl$resourcePack;

    public MinecraftServerMixin(final String name) {
        super(name);
    }

    @Override
    public Subject getSubject() {
        return SpongeCommon.getGame().getSystemSubject();
    }

    @Inject(method = "spin", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private static void impl$setThreadOnServerPhaseTracker(Function<Thread, MinecraftServer> p_240784_0_, final CallbackInfoReturnable<MinecraftServerMixin> cir,
            AtomicReference<MinecraftServer> atomicReference, Thread thread) {
        try {
            PhaseTracker.SERVER.setThread(thread);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException("Could not initialize the server PhaseTracker!");
        }
    }

    @Inject(method = "setResourcePack(Ljava/lang/String;Ljava/lang/String;)V", at = @At("HEAD") )
    private void impl$createSpongeResourcePackWrapper(final String url, final String hash, final CallbackInfo ci) {
        if (url.length() == 0) {
            this.impl$resourcePack = null;
        } else {
            try {
                this.impl$resourcePack = SpongeResourcePack.create(url, hash);
            } catch (final URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ResourcePack bridge$getResourcePack() {
        return this.impl$resourcePack;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Inject(method = "tickServer", at = @At(value = "HEAD"))
    private void impl$onServerTickStart(final CallbackInfo ci) {
        TimingsManager.FULL_SERVER_TICK.startTiming();
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void impl$tickServerScheduler(final BooleanSupplier hasTimeLeft, final CallbackInfo ci) {
        this.getScheduler().tick();
    }

    @Override
    public CommandSource bridge$getCommandSource(final Cause cause) {
        return this.shadow$createCommandSourceStack();
    }

    // The Audience of the Server is actually a Forwarding Audience - so any message sent to
    // the server will be sent to everyone connected. We therefore need to make sure we send
    // things to the right place. We consider anything done by the server as being done by the
    // system subject
    @Override
    public void bridge$addToCauseStack(final CauseStackManager.StackFrame frame) {
        frame.pushCause(Sponge.getSystemSubject());
    }

    // We want to save the username cache json, as we normally bypass it.
    @Inject(method = "saveAllChunks", at = @At("RETURN"))
    private void impl$saveUsernameCacheOnSave(
            final boolean suppressLog,
            final boolean flush,
            final boolean forced,
            final CallbackInfoReturnable<Boolean> cir) {
        ((PlayerProfileCacheBridge) this.profileCache).bridge$setCanSave(true);
        this.profileCache.save();
        ((PlayerProfileCacheBridge) this.profileCache).bridge$setCanSave(false);
    }

    /**
     * @author Zidane
     * @reason Apply our branding
     */
    @Overwrite
    public String getServerModName() {
        return "sponge";
    }

    @Inject(method = "tickServer", at = @At(value = "RETURN"))
    private void impl$completeTickCheckAnimation(final CallbackInfo ci) {
        TimingsManager.FULL_SERVER_TICK.stopTiming();
    }

    @Inject(method = "stopServer", at = @At(value = "TAIL"))
    private void impl$closeLevelSaveForOtherWorlds(final CallbackInfo ci) {
        for (final Map.Entry<RegistryKey<World>, ServerWorld> entry : this.levels.entrySet()) {
            if (entry.getKey() == World.OVERWORLD) {
                continue;
            }

            final SaveFormat.LevelSave levelSave = ((ServerWorldBridge) entry.getValue()).bridge$getLevelSave();
            try {
                levelSave.close();
            } catch (IOException e) {
                LOGGER.error("Failed to unlock level {}", levelSave.getLevelId(), e);
            }
        }
    }

    @ModifyConstant(method = "tickServer", constant = @Constant(intValue = 6000, ordinal = 0))
    private int getSaveTickInterval(final int tickInterval) {
        if (!this.shadow$isDedicatedServer()) {
            return tickInterval;
        } else if (!this.shadow$isRunning()) {
            // Don't autosave while server is stopping
            return this.tickCount + 1;
        }

        final int autoPlayerSaveInterval = SpongeConfigs.getCommon().get().world.playerAutoSaveInterval;
        if (autoPlayerSaveInterval > 0 && (this.tickCount % autoPlayerSaveInterval == 0)) {
            this.shadow$getPlayerList().saveAll();
        }

        this.saveAllChunks(true, false, false);

        // force check to fail as we handle everything above
        return this.tickCount + 1;
    }

    /**
     * @author Zidane - November, 24th 2020 - Minecraft 1.15
     * @reason To allow per-world auto-save tick intervals or disable auto-saving entirely
     */
    @Overwrite
    public boolean saveAllChunks(final boolean suppressLog, final boolean flush, final boolean isForced) {
        for (final ServerWorld world : this.shadow$getAllLevels()) {
            final SerializationBehavior serializationBehavior = ((IServerWorldInfoBridge) world.getLevelData()).bridge$getSerializationBehavior();
            boolean log = !suppressLog;

            // Not forced happens during ticks and when shutting down
            if (!isForced) {
                final InheritableConfigHandle<WorldConfig> adapter = ((IServerWorldInfoBridge) world.getLevelData()).bridge$getConfigAdapter();
                final int autoSaveInterval = adapter.get().world.autoSaveInterval;
                if (log) {
                    if (this.bridge$performAutosaveChecks()) {
                        log = adapter.get().world.logAutoSave;
                    }
                }

                // Not forced means this is an auto-save or a shut down, handle accordingly

                // If the server isn't running or we hit Vanilla's save interval, save our configs
                if (!this.shadow$isRunning() || this.tickCount % 6000 == 0) {
                    ((IServerWorldInfoBridge) world.getLevelData()).bridge$getConfigAdapter().save();
                }

                final boolean canSaveAtAll = serializationBehavior != SerializationBehavior.NONE;

                // This world is set to not save of any time, no reason to check the auto-save/etc, skip it
                if (!canSaveAtAll) {
                    continue;
                }

                // Only run auto-save skipping if the server is still running
                if (this.bridge$performAutosaveChecks()) {

                    // Do not process properties or chunks if the world is not set to do so unless the server is shutting down
                    if (autoSaveInterval <= 0 || serializationBehavior != SerializationBehavior.AUTOMATIC) {
                        continue;
                    }

                    // Now check the interval vs the tick counter and skip it
                    if (this.tickCount % autoSaveInterval != 0) {
                        continue;
                    }
                }

                world.save(null, false, world.noSave);

                if (log) {
                    if (this.bridge$performAutosaveChecks()) {
                        MinecraftServerMixin.LOGGER.info("Auto-saving data for world '{}'", ((org.spongepowered.api.world.server.ServerWorld) world).getKey());
                    } else {
                        MinecraftServerMixin.LOGGER.info("Saving data for world '{}'", ((org.spongepowered.api.world.server.ServerWorld) world).getKey());
                    }
                }
            // Forced happens during command
            } else {
                if (log) {
                    MinecraftServerMixin.LOGGER.info("Manually saving data for world '{}'", ((org.spongepowered.api.world.server.ServerWorld) world).getKey());
                }

                ((IServerWorldInfoBridge) world.getLevelData()).bridge$getConfigAdapter().save();

                world.save(null, false, world.noSave);
            }
        }

        this.storageSource.saveDataTag(this.registryHolder, this.worldData, this.shadow$getPlayerList().getSingleplayerData());

        return true;
    }

    /**
     * @author Zidane
     * @reason Set the difficulty without marking as custom
     */
    @Overwrite
    public void setDifficulty(final Difficulty difficulty, final boolean forceDifficulty) {
        for (final ServerWorld world : this.shadow$getAllLevels()) {
            ((SpongeServer) SpongeCommon.getServer()).getWorldManager().adjustWorldForDifficulty(world, difficulty, forceDifficulty);
        }
    }

    @Override
    public void bridge$initServices(final Game game, final Injector injector) {
        if (this.impl$serviceProvider == null) {
            this.impl$serviceProvider = new SpongeServerScopedServiceProvider(this, game, injector);
            this.impl$serviceProvider.init();
        }
    }

    /**
     * Render localized chat components
     *
     * @param input original component
     * @return converted message
     */
    @ModifyVariable(method = "sendMessage", at = @At("HEAD"), argsOnly = true)
    private ITextComponent impl$applyTranslation(final ITextComponent input) {
        return NativeComponentRenderer.apply(input.copy(), Locale.getDefault());
    }

    @Override
    public SpongeServerScopedServiceProvider bridge$getServiceProvider() {
        return this.impl$serviceProvider;
    }

    @Inject(method = "reloadResources", at = @At(value = "HEAD"))
    public void impl$reloadPluginRecipes(Collection<String> datapacksToLoad, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        SpongeIngredient.clearCache();
        ResultUtil.clearCache();
        SpongeDataPackManager.INSTANCE.callRegisterDataPackValueEvent();
        try {
            SpongeDataPackManager.INSTANCE.serialize(this.storageSource.getLevelPath(FolderName.DATAPACK_DIR));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        this.shadow$getPackRepository().reload();
        final List<String> disabledPacks = this.worldData.getDataPackConfig().getDisabled();
        for (String selectedPack : this.shadow$getPackRepository().getSelectedIds()) {
            if (!disabledPacks.contains(selectedPack) && !datapacksToLoad.contains(selectedPack)) {
                datapacksToLoad.add(selectedPack);
            }
        }
    }
}
