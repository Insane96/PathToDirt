package insane96mcp.pathtodirt.module.base.feature;

import insane96mcp.insanelib.base.Feature;
import insane96mcp.insanelib.base.Label;
import insane96mcp.insanelib.base.Module;
import insane96mcp.insanelib.config.Blacklist;
import insane96mcp.insanelib.util.IdTagMatcher;
import insane96mcp.insanelib.util.LogHelper;
import insane96mcp.pathtodirt.setup.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Label(name = "Dirt to Path")
public class BaseFeature extends Feature {

    private final ForgeConfigSpec.ConfigValue<List<? extends String>> transformListConfig;
    private final Blacklist.Config itemBlacklistConfig;

    private static final List<String> transformListDefault = Arrays.asList("minecraft:dirt_path,minecraft:dirt", "minecraft:farmland,minecraft:dirt");

    public ArrayList<Transform> transformList;
    public Blacklist itemBlacklist;

    public BaseFeature(Module module) {
        super(Config.builder, module, true, false);
        transformListConfig = Config.builder
                .comment("""
                        Write here a list of custom overrides when right clicking a block.
                        It must be blockRightClicked,blockToTransformTo.
                        Blocks must have the format modid:blockid.
                        E.g. minecraft:farmland,minecraft:dirt will make farmland transform to path when right-clicked with a shovel. You can even use tags in blockRightClicked.""")
                .defineList("Transform List", transformListDefault, o -> o instanceof String);
        itemBlacklistConfig = new Blacklist.Config(Config.builder, "Item Blacklist", "Items and tags that should not erform the block transformation.")
                .setDefaultList(List.of())
                .setIsDefaultWhitelist(false)
                .build();
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        this.transformList = Transform.parseList(this.transformListConfig.get());
        this.itemBlacklist = this.itemBlacklistConfig.get();
    }

    @SubscribeEvent
    public void onRightClick(BlockEvent.BlockToolModificationEvent event) {
        if (!this.isEnabled()
                || event.isSimulated()
                || event.getToolAction() != ToolActions.SHOVEL_FLATTEN
                || event.getState().getBlock().getToolModifiedState(event.getState(), event.getContext(), event.getToolAction(), true) != null
                || event.getContext() != null && !event.getContext().getLevel().isEmptyBlock(event.getPos().above())
                || this.itemBlacklist.isItemBlackOrNotWhiteListed(event.getHeldItemStack().getItem()))
            return;

        for (Transform transform : this.transformList) {
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
