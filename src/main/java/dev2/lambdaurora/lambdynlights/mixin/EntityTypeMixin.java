/*
 * Copyright © 2020-2022 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package dev2.lambdaurora.lambdynlights.mixin;

import dev2.lambdaurora.lambdynlights.LambDynLightsMod;
import dev2.lambdaurora.lambdynlights.accessor.DynamicLightHandlerHolder;
import dev2.lambdaurora.lambdynlights.api.DynamicLightHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin<T extends Entity> implements DynamicLightHandlerHolder<T> {
	@Unique
	private DynamicLightHandler<T> lambdynlights_lightHandler;
	@Unique
	private Boolean lambdynlights_settingValue = null;
	@Unique
	private String lambdynlights_settingKey = null;

	@Override
	public @Nullable DynamicLightHandler<T> lambdynlights_getDynamicLightHandler() {
		return this.lambdynlights_lightHandler;
	}

	@Override
	public void lambdynlights_setDynamicLightHandler(DynamicLightHandler<T> handler) {
		this.lambdynlights_lightHandler = handler;
	}

	@Override
	public Boolean lambdynlights_getSetting() {
		if (this.lambdynlights_settingKey == null) {
			var self = (EntityType<?>) (Object) this;
			var id = ForgeRegistries.ENTITIES.getKey(self);
			if (id.getPath().equals("pig") && self != EntityType.PIG && id.getNamespace().equals("minecraft")) {
				return null;
			}

			lambdynlights_settingKey = "light_sources.settings.entities."
					+ id.getNamespace() + '.' + id.getPath().replace('/', '.');
			lambdynlights_settingValue = LambDynLightsMod.config.handleEntity(lambdynlights_settingKey);
		}

		return lambdynlights_settingValue;
	}
}
