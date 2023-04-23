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
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity implements DynamicLightSource {
	@Shadow
	public abstract boolean isSpectator();

	@Unique
	protected int lambdynlights_luminance;
	@Unique
	private Level lambdynlights_lastWorld;

	protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
		super(entityType, world);
	}

	@Override
	public void dynamicLightTick() {
		if (!DynamicLightHandlers.canLightUp(this)) {
			this.lambdynlights_luminance = 0;
			return;
		}

		if (this.isOnFire() || this.isCurrentlyGlowing()) {
			this.lambdynlights_luminance = 15;
		} else {
			int luminance = DynamicLightHandlers.getLuminanceFrom((Entity) this);

			var eyePos = new BlockPos(this.getX(), this.getEyeY(), this.getZ());
			boolean submergedInFluid = !this.level.getFluidState(eyePos).isEmpty();
			for (var equipped : this.getAllSlots()) {
				if (!equipped.isEmpty())
					luminance = Math.max(luminance, LambDynLightsMod.getLuminanceFromItemStack(equipped, submergedInFluid));
			}

			this.lambdynlights_luminance = luminance;
		}

		if (this.isSpectator())
			this.lambdynlights_luminance = 0;

		if (this.lambdynlights_lastWorld != this.getLevel()) {
			this.lambdynlights_lastWorld = this.getLevel();
			this.lambdynlights_luminance = 0;
		}
	}

	@Override
	public int getLuminance() {
		return this.lambdynlights_luminance;
	}
}
