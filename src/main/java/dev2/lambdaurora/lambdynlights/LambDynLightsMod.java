/*
 * Licensed under GNU GPL.
 * This is a small modification of True Darkness @
 *   https://github.com/grondag/darkness and
 *   https://www.curseforge.com/minecraft/mc-mods/true-darkness,
 * to add config variables to influence the darkness parameters and implement a simple server-side enforcement.
 */
package dev2.lambdaurora.lambdynlights;

import com.mojang.logging.LogUtils;
import dev2.lambdaurora.lambdynlights.accessor.WorldRendererAccessor;
import dev2.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import dev2.lambdaurora.lambdynlights.api.item.ItemLightSources;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.client.renderer.LightTexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

@Mod(LambDynLightsMod.MOD_ID)
public final class LambDynLightsMod
{
    public static final String MOD_ID = "lambdynlights";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static DynamicLightsConfig config;

    private static final double MAX_RADIUS = 7.75;
    private static final double MAX_RADIUS_SQUARED = MAX_RADIUS * MAX_RADIUS;
    private static LambDynLightsMod INSTANCE;
    private final Set<DynamicLightSource> dynamicLightSources = new HashSet<>();
    private final ReentrantReadWriteLock lightSourcesLock = new ReentrantReadWriteLock();
    private long lastUpdate = System.currentTimeMillis();
    private int lastUpdateCount = 0;

    public LambDynLightsMod()
    {
        LOGGER.info("Initializing LambDynLightsMod");
        INSTANCE = this;

        final var cfg = new ForgeConfigSpec.Builder().configure(DynamicLightsConfig::new);
        config = cfg.getLeft();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, cfg.getRight(), MOD_ID + ".toml");

        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(config::loadOptionsFromConfigFile);
        bus.addListener(LambDynLightsMod::registerClientReloadListeners);

        MinecraftForge.EVENT_BUS.addListener(this::onRenderLevelStageEvent);


        //todo: for mods to interface to. how would Forge handle this?
//        FabricLoader.getInstance().getEntrypointContainers("dynamiclights", DynamicLightsInitializer.class)
//                .stream().map(EntrypointContainer::getEntrypoint)
//                .forEach(DynamicLightsInitializer::onInitializeDynamicLights);

        DynamicLightHandlers.registerDefaultHandlers();
    }

    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event)
    {
        event.registerReloadListener(new ItemLightSourcesReloadListener(ItemLightSources::applyItemLightSources, "dynamiclights/item"));
    }

    public void onRenderLevelStageEvent(RenderLevelStageEvent event) {
        if(event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            Minecraft.getInstance().getProfiler().popPush("dynamic_lighting");
            this.updateAll(event.getLevelRenderer());
        }
    }

    /**
     * Updates all light sources.
     *
     * @param renderer the renderer
     */
    public void updateAll(@NotNull LevelRenderer renderer) {
        int delay = config.options.dynamicLightsMode();
        if(delay < 0) return;

//        if (!config.isDynamicLightsEnabled())
//            return;

        long now = System.currentTimeMillis();
        if (now >= this.lastUpdate + 50) {
            this.lastUpdate = now;
            this.lastUpdateCount = 0;

            this.lightSourcesLock.readLock().lock();
            for (var lightSource : this.dynamicLightSources) {
                if (lightSource.lambdynlights_updateDynamicLight(renderer)) this.lastUpdateCount++;
            }
            this.lightSourcesLock.readLock().unlock();
        }
    }

    /**
     * Returns the last number of dynamic light source updates.
     *
     * @return the last number of dynamic light source updates
     */
    public int getLastUpdateCount() {
        return this.lastUpdateCount;
    }

    /**
     * Returns the lightmap with combined light levels.
     *
     * @param pos the position
     * @param lightmap the vanilla lightmap coordinates
     * @return the modified lightmap coordinates
     */
    public int getLightmapWithDynamicLight(@NotNull BlockPos pos, int lightmap) {
        return this.getLightmapWithDynamicLight(this.getDynamicLightLevel(pos), lightmap);
    }

    /**
     * Returns the lightmap with combined light levels.
     *
     * @param entity the entity
     * @param lightmap the vanilla lightmap coordinates
     * @return the modified lightmap coordinates
     */
    public int getLightmapWithDynamicLight(@NotNull Entity entity, int lightmap) {
        int posLightLevel = (int) this.getDynamicLightLevel(entity.blockPosition());
        int entityLuminance = ((DynamicLightSource) entity).getLuminance();

        return this.getLightmapWithDynamicLight(Math.max(posLightLevel, entityLuminance), lightmap);
    }

    /**
     * Returns the lightmap with combined light levels.
     *
     * @param dynamicLightLevel the dynamic light level
     * @param lightmap the vanilla lightmap coordinates
     * @return the modified lightmap coordinates
     */
    public int getLightmapWithDynamicLight(double dynamicLightLevel, int lightmap) {
        if (dynamicLightLevel > 0) {
            // lightmap is (skyLevel << 20 | blockLevel << 4)

            // Get vanilla block light level.
            int blockLevel = LightTexture.block(lightmap);
            if (dynamicLightLevel > blockLevel) {  //todo: does this quirk really matter???
                // Equivalent to a << 4 bitshift with a little quirk: this one ensure more precision (more decimals are saved).
                int luminance = (int) (dynamicLightLevel * 16.0);
                lightmap &= 0xfff00000;
                lightmap |= luminance & 0x000fffff;
            }
        }

        return lightmap;
    }

    /**
     * Returns the dynamic light level at the specified position.
     *
     * @param pos the position
     * @return the dynamic light level at the specified position
     */
    public double getDynamicLightLevel(@NotNull BlockPos pos) {
        double result = 0;
        this.lightSourcesLock.readLock().lock();
        for (var lightSource : this.dynamicLightSources) {
            result = maxDynamicLightLevel(pos, lightSource, result);
        }
        this.lightSourcesLock.readLock().unlock();

        return Mth.clamp(result, 0, 15);
    }

    /**
     * Returns the dynamic light level generated by the light source at the specified position.
     *
     * @param pos the position
     * @param lightSource the light source
     * @param currentLightLevel the current surrounding dynamic light level
     * @return the dynamic light level at the specified position
     */
    public static double maxDynamicLightLevel(@NotNull BlockPos pos, @NotNull DynamicLightSource lightSource, double currentLightLevel) {
        int luminance = lightSource.getLuminance();
        if (luminance > 0) {
            // Can't use Entity#squaredDistanceTo because of eye Y coordinate.
            double dx = pos.getX() - lightSource.getDynamicLightX() + 0.5;
            double dy = pos.getY() - lightSource.getDynamicLightY() + 0.5;
            double dz = pos.getZ() - lightSource.getDynamicLightZ() + 0.5;

            double distanceSquared = dx * dx + dy * dy + dz * dz;
            // 7.75 because else we would have to update more chunks and that's not a good idea.
            // 15 (max range for blocks) would be too much and a bit cheaty.
            if (distanceSquared <= MAX_RADIUS_SQUARED) {
                double multiplier = 1.0 - Math.sqrt(distanceSquared) / MAX_RADIUS;
                double lightLevel = multiplier * (double)luminance;
                if (lightLevel > currentLightLevel) {
                    return lightLevel;
                }
            }
        }
        return currentLightLevel;
    }

    /**
     * Adds the light source to the tracked light sources.
     *
     * @param lightSource the light source to add
     */
    public void addLightSource(@NotNull DynamicLightSource lightSource) {
        if (!lightSource.getDynamicLightWorld().isClientSide)
            return;
        if (!config.isDynamicLightsEnabled())
            return;
        if (this.containsLightSource(lightSource))
            return;
        this.lightSourcesLock.writeLock().lock();
        this.dynamicLightSources.add(lightSource);
        this.lightSourcesLock.writeLock().unlock();
    }

    /**
     * Returns whether the light source is tracked or not.
     *
     * @param lightSource the light source to check
     * @return {@code true} if the light source is tracked, else {@code false}
     */
    public boolean containsLightSource(@NotNull DynamicLightSource lightSource) {
        if (!lightSource.getDynamicLightWorld().isClientSide)
            return false;

        boolean result;
        this.lightSourcesLock.readLock().lock();
        result = this.dynamicLightSources.contains(lightSource);
        this.lightSourcesLock.readLock().unlock();
        return result;
    }

    /**
     * Returns the number of dynamic light sources that currently emit lights.
     *
     * @return the number of dynamic light sources emitting light
     */
    public int getLightSourcesCount() {
        int result;

        this.lightSourcesLock.readLock().lock();
        result = this.dynamicLightSources.size();
        this.lightSourcesLock.readLock().unlock();

        return result;
    }

    /**
     * Removes the light source from the tracked light sources.
     *
     * @param lightSource the light source to remove
     */
    public void removeLightSource(@NotNull DynamicLightSource lightSource) {
        this.lightSourcesLock.writeLock().lock();

        var dynamicLightSources = this.dynamicLightSources.iterator();
        DynamicLightSource it;
        while (dynamicLightSources.hasNext()) {
            it = dynamicLightSources.next();
            if (it.equals(lightSource)) {
                dynamicLightSources.remove();
                lightSource.lambdynlights_scheduleTrackedChunksRebuild(Minecraft.getInstance().levelRenderer);
                break;
            }
        }
        this.lightSourcesLock.writeLock().unlock();
    }

    /**
     * Clears light sources.
     */
    public void clearLightSources() {
        this.lightSourcesLock.writeLock().lock();

        var dynamicLightSources = this.dynamicLightSources.iterator();
        DynamicLightSource it;
        while (dynamicLightSources.hasNext()) {
            it = dynamicLightSources.next();
            dynamicLightSources.remove();
            if (it.getLuminance() > 0)
                it.resetDynamicLight();
            it.lambdynlights_scheduleTrackedChunksRebuild(Minecraft/*Client*/.getInstance().levelRenderer);
        }

        this.lightSourcesLock.writeLock().unlock();
    }

    /**
     * Removes light sources if the filter matches.
     *
     * @param filter the removal filter
     */
    public void removeLightSources(@NotNull Predicate<DynamicLightSource> filter) {
        this.lightSourcesLock.writeLock().lock();

        var dynamicLightSources = this.dynamicLightSources.iterator();
        DynamicLightSource it;
        while (dynamicLightSources.hasNext()) {
            it = dynamicLightSources.next();
            if (filter.test(it)) {
                dynamicLightSources.remove();
                if (it.getLuminance() > 0)
                    it.resetDynamicLight();
                it.lambdynlights_scheduleTrackedChunksRebuild(Minecraft/*Client*/.getInstance().levelRenderer);
                break;
            }
        }

        this.lightSourcesLock.writeLock().unlock();
    }

    /**
     * Removes entities light source from tracked light sources.
     */
    public void removeEntitiesLightSource() {
        this.removeLightSources(lightSource -> (lightSource instanceof Entity && !(lightSource instanceof Player)));
    }

    /**
     * Removes Creeper light sources from tracked light sources.
     */
    public void removeCreeperLightSources() {
        this.removeLightSources(entity -> entity instanceof Creeper);
    }

    /**
     * Removes TNT light sources from tracked light sources.
     */
    public void removeTntLightSources() {
        this.removeLightSources(entity -> entity instanceof PrimedTnt);
    }

    /**
     * Removes block entities light source from tracked light sources.
     */
    public void removeBlockEntitiesLightSource() {
        this.removeLightSources(lightSource -> lightSource instanceof BlockEntity);
    }

    /**
     * Prints a message to the terminal.
     *
     * @param info the message to print
     */
    public void log(String info) {
        LOGGER.info("[LambDynLights] " + info);
    }

    /**
     * Prints a warning message to the terminal.
     *
     * @param info the message to print
     */
    public void warn(String info) {
        LOGGER.warn("[LambDynLights] " + info);
    }

    /**
     * Schedules a chunk rebuild at the specified chunk position.
     *
     * @param renderer the renderer
     * @param chunkPos the chunk position
     */
    public static void scheduleChunkRebuild(@NotNull LevelRenderer renderer, @NotNull BlockPos chunkPos) {
        scheduleChunkRebuild(renderer, chunkPos.getX(), chunkPos.getY(), chunkPos.getZ());
    }

    /**
     * Schedules a chunk rebuild at the specified chunk position.
     *
     * @param renderer the renderer
     * @param chunkPos the packed chunk position
     */
    public static void scheduleChunkRebuild(@NotNull LevelRenderer renderer, long chunkPos) {
        scheduleChunkRebuild(renderer, BlockPos.getX(chunkPos), BlockPos.getY(chunkPos), BlockPos.getZ(chunkPos));
    }

    public static void scheduleChunkRebuild(@NotNull LevelRenderer renderer, int x, int y, int z) {
        if (Minecraft.getInstance().level != null)
            ((WorldRendererAccessor) renderer).lambdynlights_scheduleChunkRebuild(x, y, z, false);
    }

    /**
     * Updates the tracked chunk sets.
     *
     * @param chunkPos the packed chunk position
     * @param old the set of old chunk coordinates to remove this chunk from it
     * @param newPos the set of new chunk coordinates to add this chunk to it
     */
    public static void updateTrackedChunks(@NotNull BlockPos chunkPos, @Nullable LongOpenHashSet old, @Nullable LongOpenHashSet newPos) {
        if (old != null || newPos != null) {
            long pos = chunkPos.asLong();
            if (old != null)
                old.remove(pos);
            if (newPos != null)
                newPos.add(pos);
        }
    }

    /**
     * Updates the dynamic lights tracking.
     *
     * @param lightSource the light source
     */
    public static void updateTracking(@NotNull DynamicLightSource lightSource) {
        boolean enabled = lightSource.isDynamicLightEnabled();
        int luminance = lightSource.getLuminance();

        if (!enabled && luminance > 0) {
            lightSource.setDynamicLightEnabled(true);
        } else if (enabled && luminance < 1) {
            lightSource.setDynamicLightEnabled(false);
        }
    }

    /**
     * Returns the luminance from an item stack.
     *
     * @param stack the item stack
     * @param submergedInWater {@code true} if the stack is submerged in water, else {@code false}
     * @return the luminance of the item
     */
    public static int getLuminanceFromItemStack(@NotNull ItemStack stack, boolean submergedInWater) {
        return ItemLightSources.getLuminance(stack, submergedInWater);
    }

    /**
     * Returns the LambDynamicLights mod instance.
     *
     * @return the mod instance
     */
    public static LambDynLightsMod get() {
        return INSTANCE;
    }



}