/*
 * Licensed under MIT License.
 * This is a FORGE port (for Minecraft 1.18.2) of Lamb Dynamic Lights @
 *   https://github.com/grondag/darkness and
 *   https://www.curseforge.com/minecraft/mc-mods/lambdynamiclights
 */
package dev2.lambdaurora.lambdynlights;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.*;

/**
 * Represents the mod configuration.
 *
 * @author LambdAurora
 * @version 2.2.0
 * @since 1.0.0
 */
public class DynamicLightsConfig {
	//todo these?
	private static final  /*DynamicLightsMode*/ int DEFAULT_DYNAMIC_LIGHTS_MODE = 150;//DynamicLightsMode.FANCY;
	private static final boolean DEFAULT_ENTITIES_LIGHT_SOURCE = true;
	private static final boolean DEFAULT_SELF_LIGHT_SOURCE = true;
	private static final boolean DEFAULT_BLOCK_ENTITIES_LIGHT_SOURCE = true;
	private static final boolean DEFAULT_WATER_SENSITIVE_CHECK = true;
	private static final ExplosiveLightingMode DEFAULT_CREEPER_LIGHTING_MODE = ExplosiveLightingMode.SIMPLE;
	private static final ExplosiveLightingMode DEFAULT_TNT_LIGHTING_MODE = ExplosiveLightingMode.OFF;

	private final ConfigOptions configOptions;
	public Options options = null;
	private static final String LANGKEY_CONFIG  = "config";

	public enum Values { dynamicLightsMode, entitiesLightSource, selfLightSource, blockEntitiesLightSource, waterSensitiveCheck, tnt, creeper, block_entities, entities }
	public record Options(/*DynamicLightsMode*/int dynamicLightsMode, boolean entitiesLightSource, boolean selfLightSource, boolean blockEntitiesLightSource, boolean waterSensitiveCheck, ExplosiveLightingMode tnt, ExplosiveLightingMode creeper, List<? extends String> block_entities, List<? extends String> entities) { }
	public record ConfigOptions(IntValue dynamicLightsMode, BooleanValue entitiesLightSource, BooleanValue selfLightSource, BooleanValue blockEntitiesLightSource, BooleanValue waterSensitiveCheck, EnumValue<ExplosiveLightingMode> tnt, EnumValue<ExplosiveLightingMode> creeper, ConfigValue<List<? extends String>> block_entities, ConfigValue<List<? extends String>> entities) {
		public Options setup() {
			return new Options(
					dynamicLightsMode.get(),
					entitiesLightSource.get(),
					selfLightSource.get(),
					blockEntitiesLightSource.get(),
					waterSensitiveCheck.get(),
					tnt.get(),
					creeper.get(),
					block_entities.get(),
					entities.get());
		}
	}

	public DynamicLightsConfig(final ForgeConfigSpec.Builder builder) {

		builder.comment("  LambDynamicLightsFORGE Config").push(LambDynLightsMod.MOD_ID);
		configOptions = new ConfigOptions(
				builder.comment("", "  dynamicLightsMode")
						.translation(getLangKey(LANGKEY_CONFIG, Values.dynamicLightsMode.toString()))
						//.defineEnum(Values.dynamicLightsMode.toString(), DEFAULT_DYNAMIC_LIGHTS_MODE),
						.defineInRange(Values.dynamicLightsMode.toString(), DEFAULT_DYNAMIC_LIGHTS_MODE, -1, 1000),
				builder.comment("", "  entitiesLightSource")
						.translation(getLangKey(LANGKEY_CONFIG, Values.entitiesLightSource.toString()))
						.define(Values.entitiesLightSource.toString(), DEFAULT_ENTITIES_LIGHT_SOURCE),
				builder.comment("", "  selfLightSource")
						.translation(getLangKey(LANGKEY_CONFIG, Values.selfLightSource.toString()))
						.define(Values.selfLightSource.toString(), DEFAULT_SELF_LIGHT_SOURCE),
				builder.comment("", "  blockEntitiesLightSource")
						.translation(getLangKey(LANGKEY_CONFIG, Values.blockEntitiesLightSource.toString()))
						.define(Values.blockEntitiesLightSource.toString(), DEFAULT_BLOCK_ENTITIES_LIGHT_SOURCE),
				builder.comment("", "  waterSensitiveCheck")
						.translation(getLangKey(LANGKEY_CONFIG, Values.waterSensitiveCheck.toString()))
						.define(Values.waterSensitiveCheck.toString(), DEFAULT_WATER_SENSITIVE_CHECK),
				builder.comment("", "  tnt")
						.translation(getLangKey(LANGKEY_CONFIG, Values.tnt.toString()))
						.defineEnum(Values.tnt.toString(), DEFAULT_TNT_LIGHTING_MODE),
				builder.comment("", "  creeper")
						.translation(getLangKey(LANGKEY_CONFIG, Values.creeper.toString()))
						.defineEnum(Values.creeper.toString(), DEFAULT_CREEPER_LIGHTING_MODE),
				builder.comment("", "  block_entities")
						.translation(getLangKey(LANGKEY_CONFIG, Values.block_entities.toString()))
						.defineList(Values.block_entities.toString(), new ArrayList<>(), s -> true), //todo validate!
				builder.comment("", "  entities")
						.translation(getLangKey(LANGKEY_CONFIG, Values.entities.toString()))
						.defineList(Values.entities.toString(), new ArrayList<>(), s -> true) //todo validate!
				);
		builder.pop();
	}

	Set<String> setBlockEntities = new HashSet<>();
	Set<String> setEntities = new HashSet<>();

	public void loadOptionsFromConfigFile(@SuppressWarnings("unused") FMLCommonSetupEvent event) {
		options = configOptions.setup();
		setBlockEntities = new HashSet<>();
		setEntities = new HashSet<>();
		setBlockEntities.addAll(options.block_entities);
		setEntities.addAll(options.entities);
	}

	public boolean handleBlockEntity(String id) {
		if(setBlockEntities.contains(id))
			return true;
		if(true) { // todo: should this be a config entry, is there one? to determine the initial true/false of this entry?
			setBlockEntities.add(id);
			((List<String>)configOptions.block_entities.get()).add(id);
			configOptions.block_entities.save();
			return true;
		}
		return false;
	}

	public boolean handleEntity(String id) {
		if(setEntities.contains(id))
			return true;
		if(true) { // todo: should this be a config entry, is there one? to determine the initial true/false of this entry?
			setEntities.add(id);
			((List<String>)configOptions.entities.get()).add(id);
			configOptions.entities.save();
			return true;
		}
		return false;
	}

	public boolean isDynamicLightsEnabled() {
		return options.dynamicLightsMode >= 0;
	}


	/**
	 * A I18n key helper.
	 * -> Sourced originally from FallThru (srsCode) @ github.com/srsCode/FallThru, MIT License
	 *
	 * @param  keys A list of key elements to be joined.
	 * @return      A full I18n key.
	 */
	private static String getLangKey(final String... keys)
	{
		return (keys.length > 0) ? String.join(".", LambDynLightsMod.MOD_ID, String.join(".", keys)) : LambDynLightsMod.MOD_ID;
	}

	/**
	 * This Event handler syncs a server-side config change with all connected players.
	 * This fires when the config file has been changed on disk and only updates on the client if the
	 * client is <b>not</b> connected to a remote server, or if the integrated server <b>is</b> running.
	 * This will always cause syncing on a dedicated server that will propogate to clients.
	 * -> Sourced originally from FallThru (srsCode) @ github.com/srsCode/FallThru, MIT License
	 *
	 * @param event The {@link ModConfigEvent.Reloading} event
	 */
	public void onConfigUpdate(final ModConfigEvent.Reloading event)
	{
		if (event.getConfig().getModId().equals(LambDynLightsMod.MOD_ID)) {
//			if (FMLEnvironment.dist == Dist.CLIENT && (Minecraft.getInstance().getSingleplayerServer() == null || Minecraft.getInstance().getConnection() != null)) {
//				Mod.LOGGER.warn("The config file has changed but the integrated server is not running. Nothing to do.");
//			} else {
//				Mod.LOGGER.warn("The config file has changed and the server is running. Resyncing config.");
				LambDynLightsMod.config.loadOptionsFromConfigFile(null);
//				DistExecutor.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> NetworkHandler.INSTANCE::updateAll);
//			}
		}
	}
}
