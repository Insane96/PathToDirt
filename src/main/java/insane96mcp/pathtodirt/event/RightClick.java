package insane96mcp.pathtodirt.event;

import insane96mcp.pathtodirt.PathToDirt;
import insane96mcp.pathtodirt.setup.ModConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

@Mod.EventBusSubscriber(modid = PathToDirt.MOD_ID)
public class RightClick {

	@SubscribeEvent
	public static void eventBlockToolInteract(BlockEvent.BlockToolInteractEvent event) {
		ItemStack holding = event.getHeldItemStack();

		if (!event.getPlayer().isShiftKeyDown() && ModConfig.requireSneaking)
			return;

		//Check if the item is in the blacklist
		for (ModConfig.IdTagMatcher idTag : ModConfig.itemBlacklist) {
			if (idTag.doesItemMatch(holding.getItem()))
				return;
		}

		if (!holding.getItem().canPerformAction(holding, ToolActions.SHOVEL_FLATTEN))
			return;
		BlockState state = event.getWorld().getBlockState(event.getPos());
		ResourceLocation blockToTransformTo = blockToTransformTo(state);
		if (blockToTransformTo == null)
			return;
		if (event.getWorld().isEmptyBlock(event.getPos().above())) {
			event.setResult(Event.Result.ALLOW);
			event.setFinalState(ForgeRegistries.BLOCKS.getValue(blockToTransformTo).defaultBlockState());
		}
	}

	public static ResourceLocation blockToTransformTo(BlockState state) {
		ResourceLocation id = state.getBlock().getRegistryName();
		Set<ResourceLocation> tags = state.getBlock().getTags();
		for (ModConfig.Override override : ModConfig.overrides) {
			if (override.blockToTransform.id.equals(id))
				return override.blockToTransformTo;
			if (tags.contains(override.blockToTransform.tag))
				return override.blockToTransformTo;
		}
		return null;
	}
}
