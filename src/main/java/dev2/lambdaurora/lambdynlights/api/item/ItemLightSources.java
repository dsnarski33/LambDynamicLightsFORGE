/*
 * Copyright © 2020-2022 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package dev2.lambdaurora.lambdynlights.api.item;

import com.google.gson.JsonParser;
import dev2.lambdaurora.lambdynlights.LambDynLightsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Represents an item light sources manager.
 *
 * @author LambdAurora
 * @version 2.1.1
 * @since 1.3.0
 */
public final class ItemLightSources {
	// made these maps, made more sense. this ok? used to be ArrayLists
	private static Map<Item, ItemLightSource> ITEM_LIGHT_SOURCES = new HashMap<>();
	private static final Map<Item, ItemLightSource> STATIC_ITEM_LIGHT_SOURCES = new HashMap<>();

	private ItemLightSources() {
		throw new UnsupportedOperationException("ItemLightSources only contains static definitions.");
	}

	// helper, load's a specific item light source json asset
	private static void load(Map<Item, ItemLightSource> itemLightSources, ResourceManager resourceManager, ResourceLocation id) {
		try (var reader = new InputStreamReader(resourceManager.getResource(id).getInputStream())) {
			var json = JsonParser.parseReader(reader).getAsJsonObject();

			ItemLightSource.fromJson(id, json).ifPresent(data -> {
				if (!STATIC_ITEM_LIGHT_SOURCES.containsKey(data.item()))
					register(itemLightSources, data);
			});
		} catch (IOException | IllegalStateException e) {
			LambDynLightsMod.get().warn("Failed to load item light source \"" + id + "\".");
		}
	}

	/**
	 * Loads the item light source data from resource pack (load only, will not be live until apply).
	 *
	 * @param resourceManager The resource manager.
	 * @param location The location where the file assets are.
	 */
	public static Map<Item, ItemLightSource> load(ResourceManager resourceManager, String location) {

		Map<Item, ItemLightSource> itemLightSources = new HashMap<>();
		resourceManager.listResources(location, s -> s.endsWith(".json")).forEach(resourceLocation -> load(itemLightSources, resourceManager, resourceLocation));
		itemLightSources.putAll(STATIC_ITEM_LIGHT_SOURCES);
		return itemLightSources;
	}

	/**
	 * Applies the (previously loaded) item light source data from resource pack
	 *
	 * @param itemLightSources The loaded sources to go live.
	 */
	public static void applyItemLightSources(Map<Item, ItemLightSource> itemLightSources) {
		ITEM_LIGHT_SOURCES = itemLightSources;
	}

	/**
	 * Registers an item light source data.
	 *
	 * @param data The item light source data.
	 */
	private static void register(Map<Item, ItemLightSource> itemLightSources, ItemLightSource data) {
		ItemLightSource source = itemLightSources.get(data.item());
		if(source != null) {
			LambDynLightsMod.get().warn("Failed to register item light source \"" + data.id() + "\", duplicates item \""
					+ ForgeRegistries.ITEMS.getKey(data.item()) + "\" found in \"" + source.id() + "\".");
			return;
		}
		itemLightSources.put(data.item(), data);
	}

	/**
	 * Registers an item light source data.
	 *
	 * @param data the item light source data
	 */
	public static void registerItemLightSource(ItemLightSource data) {
		ItemLightSource source = STATIC_ITEM_LIGHT_SOURCES.get(data.item());
		if(source != null) {
			LambDynLightsMod.get().warn("Failed to register item light source \"" + data.id() + "\", duplicates item \""
					+ ForgeRegistries.ITEMS.getKey(data.item()) + "\" found in \"" + source.id() + "\".");
			return;
		}
		STATIC_ITEM_LIGHT_SOURCES.put(data.item(), data);
	}

	/**
	 * Returns the luminance of the item in the stack.
	 *
	 * @param stack the item stack
	 * @param submergedInWater {@code true} if the stack is submerged in water, else {@code false}
	 * @return a luminance value
	 */
	public static int getLuminance(ItemStack stack, boolean submergedInWater) {
		ItemLightSource source = ITEM_LIGHT_SOURCES.get(stack.getItem());
		if(source != null)
			return source.getLuminance(stack, submergedInWater);

		if (stack.getItem() instanceof BlockItem blockItem)
			return ItemLightSource.BlockItemLightSource.getLuminance(stack, blockItem.getBlock().defaultBlockState());
		else return 0;
	}
}
