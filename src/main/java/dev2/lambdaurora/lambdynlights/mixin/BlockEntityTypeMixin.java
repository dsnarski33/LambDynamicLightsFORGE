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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntityType.class)
public class BlockEntityTypeMixin<T extends BlockEntity> implements DynamicLightHandlerHolder<T> {
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
			var self = (BlockEntityType<?>) (Object) this;
			var id = ForgeRegistries.BLOCK_ENTITIES.getKey(self);
			if (id == null) {
				return null;
			}

			lambdynlights_settingKey = "light_sources.settings.block_entities."
					+ id.getNamespace() + '.' + id.getPath().replace('/', '.');
			lambdynlights_settingValue = LambDynLightsMod.config.handleBlockEntity(lambdynlights_settingKey);
		}

		return lambdynlights_settingValue;
	}
}
