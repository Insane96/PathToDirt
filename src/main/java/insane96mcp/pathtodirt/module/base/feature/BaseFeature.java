package insane96mcp.pathtodirt.module.base.feature;

import insane96mcp.insanelib.base.Feature;
import insane96mcp.insanelib.base.Label;
import insane96mcp.insanelib.base.Module;
import insane96mcp.insanelib.base.config.Blacklist;
import insane96mcp.insanelib.base.config.Config;
import insane96mcp.insanelib.base.config.LoadFeature;
import insane96mcp.insanelib.util.IdTagMatcher;
import insane96mcp.insanelib.util.LogHelper;
import insane96mcp.pathtodirt.PathToDirt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Label(name = "Dirt to Path")
@LoadFeature(module = PathToDirt.RESOURCE_PREFIX + "base", canBeDisabled = false)
public class BaseFeature extends Feature {
    private static ForgeConfigSpec.ConfigValue<List<? extends String>> transformListConfig;

    private static final List<String> transformListDefault = Arrays.asList("minecraft:dirt_path,minecraft:dirt", "minecraft:farmland,minecraft:dirt");

    public static ArrayList<Transform> transformList;

    @Config
    @Label(name = "Item Blacklist", description = "Items and tags that should not perform the block transformation.")
    public static Blacklist itemBlacklist = new Blacklist(List.of(
            new IdTagMatcher(IdTagMatcher.Type.ID, "quark:pickarang"),
            new IdTagMatcher(IdTagMatcher.Type.TAG, "quark:netherite_pickarang")
    ), false);

    public BaseFeature(Module module, boolean enabledByDefault, boolean canBeDisabled) {
        super(module, enabledByDefault, canBeDisabled);
    }

    @Override
    public void loadConfigOptions() {
        super.loadConfigOptions();
        transformListConfig = this.getBuilder()
                .comment("""
                        Write here a list of custom overrides when right clicking a block.
                        It must be blockRightClicked,blockToTransformTo.
                        Blocks must have the format modid:blockid.
                        E.g. minecraft:farmland,minecraft:dirt will make farmland transform to path when right-clicked with a shovel. You can even use tags in blockRightClicked.""")
                .defineList("Transform List", transformListDefault, o -> o instanceof String);
    }

    @Override
    public void readConfig(final ModConfigEvent event) {
        super.readConfig(event);
        transformList = Transform.parseList(transformListConfig.get());
    }

    @SubscribeEvent
    public void onRightClick(BlockEvent.BlockToolModificationEvent event) {
        if (!this.isEnabled()
                || event.isSimulated()
                || event.getToolAction() != ToolActions.SHOVEL_FLATTEN
                || event.getState().getBlock().getToolModifiedState(event.getState(), event.getContext(), event.getToolAction(), true) != null
                || !event.getContext().getLevel().isEmptyBlock(event.getPos().above())
                || itemBlacklist.isItemBlackOrNotWhiteListed(event.getHeldItemStack().getItem()))
            return;

        for (Transform transform : transformList) {
            if (!transform.blockFrom.matchesBlock(event.getState().getBlock()))
                continue;

            event.setResult(Event.Result.ALLOW);
            event.setFinalState(transform.blockTo.defaultBlockState());
            break;
        }
    }

    public static class Transform {
        public IdTagMatcher blockFrom;
        public Block blockTo;

        public Transform(IdTagMatcher blockFrom, Block blockTo) {
            this.blockFrom = blockFrom;
            this.blockTo = blockTo;
        }

        public static ArrayList<Transform> parseList(List<? extends String> list) {
            ArrayList<Transform> tranforms = new ArrayList<>();
            for (String line : list) {
                String[] split = line.split(",");
                if (split.length != 2) {
                    LogHelper.warn("Invalid line \"%s\". Format must be modid:tag_or_block_id,modid:block_id", line);
                    continue;
                }

                IdTagMatcher idTagMatcher = IdTagMatcher.parseLine(split[0]);
                if (idTagMatcher == null) {
                    LogHelper.warn("Invalid Id or Tag \"%s\"", split[0]);
                    continue;
                }

                ResourceLocation resourceLocation = ResourceLocation.tryParse(split[1]);
                if (resourceLocation == null) {
                    LogHelper.warn("%s id is not valid", split[1]);
                    continue;
                }
                Block block = ForgeRegistries.BLOCKS.getValue(resourceLocation);
                if (block == null) {
                    LogHelper.warn("%s block doesn't exist", split[1]);
                    continue;
                }

                tranforms.add(new Transform(idTagMatcher, block));
            }
            return tranforms;
        }
    }
}
