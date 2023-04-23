/*
 * Copyright © 2020-2022 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package dev2.lambdaurora.lambdynlights.accessor;

import dev2.lambdaurora.lambdynlights.api.DynamicLightHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
@ApiStatus.NonExtendable
public interface DynamicLightHandlerHolder<T> {
	@Nullable DynamicLightHandler<T> lambdynlights_getDynamicLightHandler();

	void lambdynlights_setDynamicLightHandler(DynamicLightHandler<T> handler);

	Boolean lambdynlights_getSetting();

	@SuppressWarnings("unchecked")
	static <T extends Entity> DynamicLightHandlerHolder<T> cast(EntityType<T> entityType) {
		return (DynamicLightHandlerHolder<T>) entityType;
	}

	@SuppressWarnings("unchecked")
	static <T extends BlockEntity> DynamicLightHandlerHolder<T> cast(BlockEntityType<T> entityType) {
		return (DynamicLightHandlerHolder<T>) entityType;
	}
}
