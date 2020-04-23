package insane96mcp.pathtodirt.event;

import insane96mcp.pathtodirt.PathToDirt;
import insane96mcp.pathtodirt.setup.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

@Mod.EventBusSubscriber(modid = PathToDirt.MOD_ID)
public class RightClick {

	@SubscribeEvent
	public static void eventRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		ItemStack holding = event.getItemStack();
		if (!holding.getItem().getToolTypes(holding).contains(ToolType.SHOVEL))
			return;
		BlockState state = event.getWorld().getBlockState(event.getPos());
		ResourceLocation blockToTransformTo = blockToTransformTo(state);
		if (blockToTransformTo == null)
			return;
		if (event.getWorld().isAirBlock(event.getPos().up())) {
			event.getWorld().playSound(event.getPlayer(), event.getPos(), SoundEvents.ITEM_SHOVEL_FLATTEN, SoundCategory.BLOCKS, 1.0F, 1.0F);
			event.setResult(Event.Result.ALLOW);
			event.getWorld().setBlockState(event.getPos(), ForgeRegistries.BLOCKS.getValue(blockToTransformTo).getDefaultState());
			event.getPlayer().swingArm(event.getHand());
			if (!event.getWorld().isRemote) {
				if (!event.getPlayer().isCreative())
					holding.damageItem(1, event.getPlayer(), player -> player.sendBreakAnimation(event.getHand()));
			}

			event.setCanceled(true);
			event.setCancellationResult(ActionResultType.SUCCESS);
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
