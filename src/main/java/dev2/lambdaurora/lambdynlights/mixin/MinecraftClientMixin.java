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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to MinecraftClient.
 * <p>
 * Goal: clear light sources cache when changing world.
 *
 * @author LambdAurora
 * @version 1.3.2
 * @since 1.3.2
 */
@Mixin(Minecraft.class)
public class MinecraftClientMixin {
	@Inject(method = "setLevel", at = @At("HEAD"))
	private void onSetWorld(ClientLevel world, CallbackInfo ci) {
		LambDynLightsMod.get().clearLightSources();
	}
}
