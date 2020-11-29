package insane96mcp.pathtodirt.setup;

import insane96mcp.pathtodirt.PathToDirt;
import net.minecraft.item.Item;
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
	public static List<IdTagMatcher> itemBlacklist = new ArrayList<>();

	private static void load() {
		List<? extends String> list = Config.CommonConfig.overrides.get();
		for (String entry : list) {
			String[] split = entry.split(",");
			if (split.length != 2) {
				PathToDirt.LOGGER.warn(String.format("Invalid line \"%s\" for Overrides", entry));
				continue;
			}
			ResourceLocation blockToTransform = null, tagToTransform = null;
			if (split[0].startsWith("#")) {
				String replaced = split[0].replace("#", "");
				tagToTransform = ResourceLocation.tryCreate(replaced);
				if (tagToTransform == null) {
					PathToDirt.LOGGER.warn(String.format("%s tag for Overrides is not valid", replaced));
					continue;
				}
			}
			else {
				blockToTransform = ResourceLocation.tryCreate(split[0]);
				if (blockToTransform == null) {
					PathToDirt.LOGGER.warn(String.format("%s block to transform for Overrides is not valid", split[0]));
					continue;
				}
				if (!ForgeRegistries.BLOCKS.containsKey(blockToTransform)) {
					PathToDirt.LOGGER.warn(String.format("%s block to transform for Overrides seems to not exist", split[0]));
					continue;
				}
			}
			IdTagMatcher idTagToTransform = new IdTagMatcher(blockToTransform, tagToTransform);
			ResourceLocation blockToTransformTo = new ResourceLocation(split[1]);
			if (blockToTransform == null) {
				PathToDirt.LOGGER.warn(String.format("%s block to transform to for Overrides is not valid", split[1]));
				continue;
			}
			if (!ForgeRegistries.BLOCKS.containsKey(blockToTransformTo)) {
				PathToDirt.LOGGER.warn(String.format("%s block to transform to for Overrides seems to not exist", split[1]));
				continue;
			}
			Override override = new Override(idTagToTransform, blockToTransformTo);
			overrides.add(override);
		}

		list = Config.CommonConfig.itemBlacklist.get();
		for (String entry : list) {
			ResourceLocation tag = null, item = null;
			if (entry.startsWith("#")) {
				String replaced = entry.replace("#", "");
				tag = ResourceLocation.tryCreate(replaced);
				if (tag == null) {
					PathToDirt.LOGGER.warn(String.format("%s tag for Item Blacklist is not valid", replaced));
					continue;
				}
			}
			else {
				item = ResourceLocation.tryCreate(entry);
				if (item == null) {
					PathToDirt.LOGGER.warn(String.format("%s item for Item Blacklist is not valid", entry));
					continue;
				}
				if (!ForgeRegistries.ITEMS.containsKey(item)) {
					PathToDirt.LOGGER.warn(String.format("%s item for Item blacklist seems to not exist", entry));
					continue;
				}
			}
			IdTagMatcher itemTagBlacklisted = new IdTagMatcher(item, tag);
			itemBlacklist.add(itemTagBlacklisted);
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

		/*
		 * Returns true if the item provided matches either the item id or is in the tag
		 */
		public boolean doesItemMatch(Item item) {
			boolean matches = false;
			matches |= item.getRegistryName().equals(this.id);
			matches |= item.getTags().contains(this.tag);
			return matches;
		}
	}

	@SubscribeEvent
	public static void onModConfigEvent(final net.minecraftforge.fml.config.ModConfig.ModConfigEvent event) {
		ModConfig.load();
	}
}
