/*
 * Copyright © 2020-2022 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package dev2.lambdaurora.lambdynlights.mixin.lightsource;

import dev2.lambdaurora.lambdynlights.DynamicLightSource;
import dev2.lambdaurora.lambdynlights.LambDynLightsMod;
import dev2.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin implements DynamicLightSource {
	@Shadow
	public Level level;

	@Shadow
	public abstract double getX();

	@Shadow
	public abstract double getEyeY();

	@Shadow
	public abstract double getZ();

	@Shadow
	public abstract double getY();

	@Shadow
	public abstract boolean isOnFire();

	@Shadow
	public abstract EntityType<?> getType();

	@Shadow
	public abstract BlockPos blockPosition();

	@Shadow
	public abstract boolean isRemoved();

	@Shadow
	public abstract ChunkPos chunkPosition();

	@Unique
	protected int lambdynlights_luminance = 0;
	@Unique
	private int lambdynlights_lastLuminance = 0;
	@Unique
	private long lambdynlights_lastUpdate = 0;
	@Unique
	private double lambdynlights_prevX;
	@Unique
	private double lambdynlights_prevY;
	@Unique
	private double lambdynlights_prevZ;
	@Unique
	private LongOpenHashSet lambdynlights_trackedLitChunkPos = new LongOpenHashSet();

	@Inject(method = "tick", at = @At("TAIL"))
	public void onTick(CallbackInfo ci) {
		// We do not want to update the entity on the server.
		if (this.level.isClientSide) {
			if (this.isRemoved()) {
				this.setDynamicLightEnabled(false);
			} else {
				this.dynamicLightTick();
				if ((!LambDynLightsMod.config.options.entitiesLightSource() && this.getType() != EntityType.PLAYER)
						|| !DynamicLightHandlers.canLightUp((Entity) (Object) this))
					this.lambdynlights_luminance = 0;
				LambDynLightsMod.updateTracking(this);
			}
		}
	}

	@Inject(method = "remove", at = @At("TAIL"))
	public void onRemove(CallbackInfo ci) {
		if (this.level.isClientSide)
			this.setDynamicLightEnabled(false);
	}

	@Override
	public double getDynamicLightX() {
		return this.getX();
	}

	@Override
	public double getDynamicLightY() {
		return this.getEyeY();
	}

	@Override
	public double getDynamicLightZ() {
		return this.getZ();
	}

	@Override
	public Level getDynamicLightWorld() {
		return this.level;
	}

	@Override
	public void resetDynamicLight() {
		this.lambdynlights_lastLuminance = 0;
	}

	@Override
	public boolean shouldUpdateDynamicLight() {
		int mode = LambDynLightsMod.config.options.dynamicLightsMode();
//		if (!mode.isEnabled())
//			return false;
		if(mode < 0) return false;
		long currentTime = System.currentTimeMillis();
		if(currentTime < this.lambdynlights_lastUpdate + mode)
			return false;
		this.lambdynlights_lastUpdate = currentTime;
		return true;
//		if (mode.hasDelay()) {
//			long currentTime = System.currentTimeMillis();
//			if (currentTime < this.lambdynlights_lastUpdate + mode.getDelay()) {
//				return false;
//			}
//
//			this.lambdynlights_lastUpdate = currentTime;
//		}
//		return true;
	}

	@Override
	public void dynamicLightTick() {
		this.lambdynlights_luminance = this.isOnFire() ? 15 : 0;

		int luminance = DynamicLightHandlers.getLuminanceFrom((Entity) (Object) this);
		if (luminance > this.lambdynlights_luminance)
			this.lambdynlights_luminance = luminance;
	}

	@Override
	public int getLuminance() {
		return this.lambdynlights_luminance;
	}

	@Override
	public boolean lambdynlights_updateDynamicLight(@NotNull LevelRenderer renderer) {
		if (!this.shouldUpdateDynamicLight())
			return false;
		double deltaX = this.getX() - this.lambdynlights_prevX;
		double deltaY = this.getY() - this.lambdynlights_prevY;
		double deltaZ = this.getZ() - this.lambdynlights_prevZ;

		int luminance = this.getLuminance();

		if (Math.abs(deltaX) > 0.1D || Math.abs(deltaY) > 0.1D || Math.abs(deltaZ) > 0.1D || luminance != this.lambdynlights_lastLuminance) {
			this.lambdynlights_prevX = this.getX();
			this.lambdynlights_prevY = this.getY();
			this.lambdynlights_prevZ = this.getZ();
			this.lambdynlights_lastLuminance = luminance;

			var newPos = new LongOpenHashSet();
			if (luminance > 0) {
				var entityChunkPos = this.chunkPosition();
				var chunkPos = new BlockPos.MutableBlockPos(entityChunkPos.x, SectionPos.posToSectionCoord(this.getEyeY()), entityChunkPos.z);

				LambDynLightsMod.scheduleChunkRebuild(renderer, chunkPos);
				LambDynLightsMod.updateTrackedChunks(chunkPos, this.lambdynlights_trackedLitChunkPos, newPos);

				var directionX = (this.blockPosition().getX() & 15) >= 8 ? Direction.EAST : Direction.WEST;
				var directionY = (Mth.floor(this.getEyeY()) & 15) >= 8 ? Direction.UP : Direction.DOWN;
				var directionZ = (this.blockPosition().getZ() & 15) >= 8 ? Direction.SOUTH : Direction.NORTH;

				for (int i = 0; i < 7; i++) {
					if (i % 4 == 0) {
						chunkPos.move(directionX); // X
					} else if (i % 4 == 1) {
						chunkPos.move(directionZ); // XZ
					} else if (i % 4 == 2) {
						chunkPos.move(directionX.getOpposite()); // Z
					} else {
						chunkPos.move(directionZ.getOpposite()); // origin
						chunkPos.move(directionY); // Y
					}
					LambDynLightsMod.scheduleChunkRebuild(renderer, chunkPos);
					LambDynLightsMod.updateTrackedChunks(chunkPos, this.lambdynlights_trackedLitChunkPos, newPos);
				}
			}

			// Schedules the rebuild of removed chunks.
			this.lambdynlights_scheduleTrackedChunksRebuild(renderer);
			// Update tracked lit chunks.
			this.lambdynlights_trackedLitChunkPos = newPos;
			return true;
		}
		return false;
	}

	//todo would this want to make sure we need to adjust the light levels first?
	@Override
	public void lambdynlights_scheduleTrackedChunksRebuild(@NotNull LevelRenderer renderer) {
		if (Minecraft.getInstance().level == this.level)
			for (long pos : this.lambdynlights_trackedLitChunkPos) {
				LambDynLightsMod.scheduleChunkRebuild(renderer, pos);
			}
	}
}
