package insane96mcp.pathtodirt.module.base.feature;

import insane96mcp.insanelib.base.Feature;
import insane96mcp.insanelib.base.Label;
import insane96mcp.insanelib.base.Module;
import insane96mcp.insanelib.config.BlacklistConfig;
import insane96mcp.insanelib.util.IdTagMatcher;
import insane96mcp.insanelib.util.LogHelper;
import insane96mcp.pathtodirt.setup.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Label(name = "Dirt to Path")
public class BaseFeature extends Feature {

    private final ForgeConfigSpec.ConfigValue<List<? extends String>> transformListConfig;
    private final BlacklistConfig itemBlacklistConfig;

    private static final List<String> transformListDefault = Arrays.asList("minecraft:dirt_path,minecraft:dirt", "minecraft:farmland,minecraft:dirt");

    public ArrayList<Transform> transformList;
    public ArrayList<IdTagMatcher> itemBlacklist;
    public Boolean itemBlacklistAsWhitelist = false;

    public BaseFeature(Module module) {
        super(Config.builder, module, true, false);
        transformListConfig = Config.builder
                .comment("""
                        Write here a list of custom overrides when right clicking a block.
                        It must be blockRightClicked,blockToTransformTo.
                        Blocks must have the format modid:blockid.
                        E.g. minecraft:farmland,minecraft:dirt will make farmland transform to path when right-clicked with a shovel. You can even use tags in blockRightClicked.""")
                .defineList("Transform List", transformListDefault, o -> o instanceof String);
        itemBlacklistConfig = new BlacklistConfig(Config.builder, "Item Blacklist", "Items and tags that should not erform the block transformation.", Arrays.asList(), false);
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        this.transformList = Transform.parseList(this.transformListConfig.get());
        this.itemBlacklist = (ArrayList<IdTagMatcher>) IdTagMatcher.parseStringList(this.itemBlacklistConfig.listConfig.get());
        this.itemBlacklistAsWhitelist = this.itemBlacklistConfig.listAsWhitelistConfig.get();
    }

    @SubscribeEvent
    public void onRightClick(BlockEvent.BlockToolModificationEvent event) {
        if (!this.isEnabled())
            return;
        if (event.isSimulated())
            return;
        if (event.getToolAction() != ToolActions.SHOVEL_FLATTEN)
            return;
        if (event.getState().getBlock().getToolModifiedState(event.getState(), event.getContext(), event.getToolAction(), true) != null)
            return;
        if (event.getContext() != null && !event.getContext().getLevel().isEmptyBlock(event.getPos().above()))
            return;

        boolean isInWhitelist = false;
        boolean isInBlacklist = false;
        for (IdTagMatcher blacklistEntry : this.itemBlacklist) {
            if (blacklistEntry.matchesItem(event.getHeldItemStack().getItem(), null)) {
                if (!this.itemBlacklistAsWhitelist)
                    isInBlacklist = true;
                else
                    isInWhitelist = true;
                break;
            }
        }
        if (isInBlacklist || (!isInWhitelist && this.itemBlacklistAsWhitelist))
            return;

        for (Transform transform : this.transformList) {
            if (!transform.blockFrom.matchesBlock(event.getState().getBlock()))
                continue;

            event.setResult(Event.Result.ALLOW);
            event.setFinalState(ForgeRegistries.BLOCKS.getValue(transform.blockTo).defaultBlockState());
            break;
        }
    }

    public static class Transform {
        public IdTagMatcher blockFrom;
        public ResourceLocation blockTo;

        public Transform(IdTagMatcher blockFrom, ResourceLocation blockTo) {
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

                tranforms.add(new Transform(idTagMatcher, resourceLocation));
            }
            return tranforms;
        }
    }
}
