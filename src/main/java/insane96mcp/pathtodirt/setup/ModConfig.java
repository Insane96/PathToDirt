package insane96mcp.pathtodirt.setup;

import insane96mcp.pathtodirt.PathToDirt;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = PathToDirt.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModConfig {

	public static List<Override> overrides = new ArrayList<>();

	private static void load() {
		List<? extends String> list = Config.CommonConfig.overrides.get();
		for (String entry : list) {
			String[] split = entry.split(",");
			if (split.length != 2) {
				PathToDirt.LOGGER.warn("Invalid line \"%s\" for Overrides");
				continue;
			}
			ResourceLocation blockToTransform = null, tagToTransform = null;
			if (split[0].startsWith("#")) {
				String replaced = split[0].replace("#", "");
				if (!ResourceLocation.isResouceNameValid(replaced)) {
					PathToDirt.LOGGER.warn("%s tag for Overrides is not valid", replaced);
					continue;
				}
				tagToTransform = new ResourceLocation(replaced);
			}
			else {
				if (!ResourceLocation.isResouceNameValid(split[0])) {
					PathToDirt.LOGGER.warn("%s block for Overrides is not valid", split[0]);
					continue;
				}
				blockToTransform = new ResourceLocation(split[0]);
				if (!ForgeRegistries.BLOCKS.containsKey(blockToTransform)) {
					PathToDirt.LOGGER.warn(String.format("%s item for Overrides seems to not exist", split[0]));
					continue;
				}
			}
			IdTagMatcher idTagToTransform = new IdTagMatcher(blockToTransform, tagToTransform);
			if (!ResourceLocation.isResouceNameValid(split[1])) {
				PathToDirt.LOGGER.warn("%s block for Overrides is not valid", split[1]);
				continue;
			}
			ResourceLocation blockToTransformTo = new ResourceLocation(split[1]);
			if (!ForgeRegistries.BLOCKS.containsKey(blockToTransformTo)) {
				PathToDirt.LOGGER.warn(String.format("%s item for Overrides seems to not exist", split[1]));
				continue;
			}
			Override override = new Override(idTagToTransform, blockToTransformTo);
			overrides.add(override);
		}
	}

	public static class Override {
		public IdTagMatcher blockToTransform;
		public ResourceLocation blockToTransformTo;

		public Override(IdTagMatcher blockToTransform, ResourceLocation blockToTransformTo) {
			this.blockToTransform = blockToTransform;
			this.blockToTransformTo = blockToTransformTo;
		}
	}

	public static class IdTagMatcher {
		public ResourceLocation id;
		public ResourceLocation tag;

		public IdTagMatcher(@Nullable ResourceLocation id, @Nullable ResourceLocation tag) {
			if (id == null && tag == null) {
				throw new NullPointerException("block and tag can't be both null");
			}
			this.id = id;
			this.tag = tag;
		}
	}

	@SubscribeEvent
	public static void onModConfigEvent(final net.minecraftforge.fml.config.ModConfig.ModConfigEvent event) {
		ModConfig.load();
	}
}
