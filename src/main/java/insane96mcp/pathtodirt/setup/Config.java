package insane96mcp.pathtodirt.setup;

import insane96mcp.pathtodirt.PathToDirt;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = PathToDirt.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

	public static final ForgeConfigSpec COMMON_SPEC;
	public static final CommonConfig COMMON;

	static {
		final Pair<CommonConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
		COMMON = specPair.getLeft();
		COMMON_SPEC = specPair.getRight();
	}

	public static class CommonConfig {
		public static ConfigValue<List<? extends String>> overrides;
		public static ConfigValue<List<? extends String>> itemBlacklist;
		public static ConfigValue<Boolean> requireSneaking;

		public CommonConfig(final ForgeConfigSpec.Builder builder) {
			overrides = builder
					.comment("Write here a list of custom overrides when right clicking a block. It must be blockRightClicked,blockToTransformTo. Blocks must have the format modid:blockid. E.g. minecraft:coarse_dirt,minecraft:grass_path will make coarse dirt transform to path when right-clicked with a shovel. You can even use tags.")
					.defineList("Overrides", Arrays.asList("minecraft:dirt,minecraft:grass_path", "minecraft:podzol,minecraft:grass_path", "minecraft:grass_path,minecraft:dirt", "minecraft:farmland,minecraft:dirt"), s -> s instanceof String);
			itemBlacklist = builder
					.comment("Write here a list of items that shouldn't execute mod's path to dirt code. Items must have the format modid:itemid. Item Tags can be used.")
					.defineList("Item Blacklist", Arrays.asList("quark:pickarang", "quark:flamerang"), s -> s instanceof String);
			requireSneaking = builder
					.comment("If true the player must be sneaking to make the mod work.")
					.define("Require Sneaking", false);
		}
	}
}