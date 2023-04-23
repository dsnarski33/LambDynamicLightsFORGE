/*
 * Copyright © 2020-2022 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package dev2.lambdaurora.lambdynlights.api;

import dev2.lambdaurora.lambdynlights.api.item.ItemLightSource;
import dev2.lambdaurora.lambdynlights.api.item.ItemLightSources;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;

/**
 * Represents the entrypoint for LambDynamicLights API.
 *
 * @author LambdAurora
 * @version 1.3.2
 * @since 1.3.2
 */
public interface DynamicLightsInitializer {
	/**
	 * Method called when LambDynamicLights is initialized to register custom dynamic light handlers and item light sources.
	 *
	 * @see DynamicLightHandlers#registerDynamicLightHandler(EntityType, DynamicLightHandler)
	 * @see DynamicLightHandlers#registerDynamicLightHandler(BlockEntityType, DynamicLightHandler)
	 * @see ItemLightSources#registerItemLightSource(ItemLightSource)
	 */
	void onInitializeDynamicLights();
}
