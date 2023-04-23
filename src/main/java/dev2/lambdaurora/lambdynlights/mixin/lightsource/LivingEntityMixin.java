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
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements DynamicLightSource {
	@Unique
	protected int lambdynlights_luminance;

	public LivingEntityMixin(EntityType<?> type, Level world) {
		super(type, world);
	}

	@Override
	public void dynamicLightTick() {
		if (!LambDynLightsMod.config.options.entitiesLightSource() || !DynamicLightHandlers.canLightUp(this)) {
			this.lambdynlights_luminance = 0;
			return;
		}

		if (this.isOnFire() || this.isCurrentlyGlowing()) {
			this.lambdynlights_luminance = 15;
		} else {
			int luminance = 0;
			var eyePos = new BlockPos(this.getX(), this.getEyeY(), this.getZ());
			boolean submergedInFluid = !this.level.getFluidState(eyePos).isEmpty();
			for (var equipped : this.getAllSlots()) {
				if (!equipped.isEmpty())
					luminance = Math.max(luminance, LambDynLightsMod.getLuminanceFromItemStack(equipped, submergedInFluid));
			}

			this.lambdynlights_luminance = luminance;
		}

		int luminance = DynamicLightHandlers.getLuminanceFrom(this);
		if (luminance > this.lambdynlights_luminance)
			this.lambdynlights_luminance = luminance;
	}

	@Override
	public int getLuminance() {
		return this.lambdynlights_luminance;
	}
}
