package insane96mcp.pathtodirt.module.base.feature;

import insane96mcp.insanelib.base.Feature;
import insane96mcp.insanelib.base.Label;
import insane96mcp.insanelib.base.LoadFeature;
import insane96mcp.insanelib.base.Module;
import insane96mcp.insanelib.base.config.Blacklist;
import insane96mcp.insanelib.base.config.Config;
import insane96mcp.insanelib.data.IdTagMatcher;
import insane96mcp.insanelib.util.LogHelper;
import insane96mcp.pathtodirt.PathToDirt;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Label(name = "Path to Dirt")
@LoadFeature(module = PathToDirt.RESOURCE_PREFIX + "base", canBeDisabled = false)
public class BaseFeature extends Feature {
    private static ForgeConfigSpec.ConfigValue<List<? extends String>> transformListConfig;

    private static final List<String> transformListDefault = Arrays.asList("minecraft:dirt_path>minecraft:dirt", "minecraft:farmland>minecraft:dirt");

    public static ArrayList<Transform> transformList;

    @Config
    @Label(name = "Item Blacklist", description = "Items and tags that should not perform the block transformation. By default any item that uses ToolActions.SHOVEL_FLATTEN will work. Note that items in this list will only be prevented from executing the transformations above and not the default Dirt to Path transformation.")
    public static Blacklist itemBlacklist = new Blacklist(List.of(
            IdTagMatcher.newId("quark:pickarang"),
            IdTagMatcher.newId("quark:netherite_pickarang")
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
                        Format: blockFrom>blockTo
                        Both sides support optional block state properties: blockFrom[prop=val,prop2=val2]>blockTo[prop=val]
                        blockFrom can be a tag (prefixed with #), but tags cannot be combined with state properties.
                        If no state properties are specified on the source side, all states of that block match.
                        If no state properties are specified on the target side, the default state is used.
                        E.g. minecraft:farmland>minecraft:dirt will make farmland transform to dirt when right-clicked with a shovel.
                        E.g. minecraft:farmland[mositure=7]>minecraft:dirt will only match wet farmland.""")
                .defineList("Transformations List", transformListDefault, o -> o instanceof String);
    }

    @Override
    public void readConfig(final ModConfigEvent event) {
        super.readConfig(event);
        transformList = Transform.parseList(transformListConfig.get());
    }

    @SubscribeEvent
    public void onRightClick(BlockEvent.BlockToolModificationEvent event) {
        if (!this.isEnabled()
                || event.getLevel().isClientSide()
                || event.isSimulated()
                || event.getToolAction() != ToolActions.SHOVEL_FLATTEN
                || event.getState().getBlock().getToolModifiedState(event.getState(), event.getContext(), event.getToolAction(), true) != null
                || !event.getContext().getLevel().isEmptyBlock(event.getPos().above())
                || itemBlacklist.isItemBlackOrNotWhiteListed(event.getHeldItemStack().getItem()))
            return;

        for (Transform transform : transformList) {
            if (!transform.matches(event.getState()))
                continue;

            event.setResult(Event.Result.ALLOW);
            if (event.getPlayer() != null)
                event.getPlayer().swing(event.getContext().getHand(), true);
            event.getLevel().playSound(null, event.getPos(), SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
            event.setFinalState(transform.stateTo);
            break;
        }
    }

    public static class Transform {
        @Nullable public Block blockFrom;
        @Nullable public TagKey<Block> tagFrom;
        public List<BlockState> statesFrom;
        public BlockState stateTo;

        public Transform(@Nullable Block blockFrom, @Nullable TagKey<Block> tagFrom, List<BlockState> statesFrom, BlockState stateTo) {
            this.blockFrom = blockFrom;
            this.tagFrom = tagFrom;
            this.statesFrom = statesFrom;
            this.stateTo = stateTo;
        }

        public boolean matches(BlockState state) {
            if (this.blockFrom != null) {
                if (!state.is(this.blockFrom))
                    return false;
                if (this.statesFrom.isEmpty())
                    return true;
                return this.statesFrom.contains(state);
            }
            return state.is(this.tagFrom);
        }

        public static ArrayList<Transform> parseList(List<? extends String> list) {
            ArrayList<Transform> transforms = new ArrayList<>();
            for (String line : list) {
                String[] parts = line.split(">", 2);
                if (parts.length != 2) {
                    LogHelper.warn("Invalid line \"%s\". Format must be blockFrom>blockTo", line);
                    continue;
                }

                String fromStr = parts[0].trim();
                String toStr = parts[1].trim();

                // Extract optional [prop=val,...] from source side
                String fromId;
                String fromProps = null;
                int fromBracket = fromStr.indexOf('[');
                if (fromBracket != -1) {
                    fromId = fromStr.substring(0, fromBracket);
                    int end = fromStr.endsWith("]") ? fromStr.length() - 1 : fromStr.length();
                    fromProps = fromStr.substring(fromBracket + 1, end);
                } else {
                    fromId = fromStr;
                }

                // Extract optional [prop=val,...] from target side
                String toId;
                String toProps = null;
                int toBracket = toStr.indexOf('[');
                if (toBracket != -1) {
                    toId = toStr.substring(0, toBracket);
                    int end = toStr.endsWith("]") ? toStr.length() - 1 : toStr.length();
                    toProps = toStr.substring(toBracket + 1, end);
                } else {
                    toId = toStr;
                }

                // Resolve target block (tags not allowed on target side)
                if (toId.startsWith("#")) {
                    LogHelper.warn("Tags are not allowed on the target side of \"%s\"", line);
                    continue;
                }
                ResourceLocation toLocation = ResourceLocation.tryParse(toId);
                if (toLocation == null) {
                    LogHelper.warn("%s is not a valid resource location", toId);
                    continue;
                }
                Block toBlock = ForgeRegistries.BLOCKS.getValue(toLocation);
                if (toBlock == null) {
                    LogHelper.warn("%s block doesn't exist", toId);
                    continue;
                }

                // Build target BlockState: start from default and apply each specified property
                BlockState stateTo = toBlock.defaultBlockState();
                if (toProps != null) {
                    try {
                        PropertiesAndValues toPav = PropertiesAndValues.of(toBlock.getStateDefinition(), toProps);
                        for (PropertyAndValue<?> pav : toPav)
                            stateTo = applyProperty(stateTo, pav);
                    } catch (Exception e) {
                        LogHelper.warn("Failed to parse target state properties for \"%s\": %s", line, e.getMessage());
                        continue;
                    }
                }

                // Resolve source side
                Block blockFrom = null;
                TagKey<Block> tagFrom = null;
                List<BlockState> statesFrom = new ArrayList<>();

                if (fromId.startsWith("#")) {
                    if (fromProps != null) {
                        LogHelper.warn("Tags cannot be combined with state properties in \"%s\"", line);
                        continue;
                    }
                    ResourceLocation tagLocation = ResourceLocation.tryParse(fromId.substring(1));
                    if (tagLocation == null) {
                        LogHelper.warn("%s is not a valid tag resource location", fromId);
                        continue;
                    }
                    tagFrom = TagKey.create(Registries.BLOCK, tagLocation);
                } else {
                    ResourceLocation fromLocation = ResourceLocation.tryParse(fromId);
                    if (fromLocation == null) {
                        LogHelper.warn("%s is not a valid resource location", fromId);
                        continue;
                    }
                    blockFrom = ForgeRegistries.BLOCKS.getValue(fromLocation);
                    if (blockFrom == null) {
                        LogHelper.warn("%s block doesn't exist", fromId);
                        continue;
                    }
                    if (fromProps != null) {
                        try {
                            PropertiesAndValues fromPav = PropertiesAndValues.of(blockFrom.getStateDefinition(), fromProps);
                            blockFrom.getStateDefinition().getPossibleStates().forEach(state -> {
                                if (fromPav.match(state))
                                    statesFrom.add(state);
                            });
                        } catch (Exception e) {
                            LogHelper.warn("Failed to parse source state properties for \"%s\": %s", line, e.getMessage());
                            continue;
                        }
                        if (statesFrom.isEmpty()) {
                            LogHelper.warn("No block states matched the properties for \"%s\"", line);
                            continue;
                        }
                    }
                }

                transforms.add(new Transform(blockFrom, tagFrom, statesFrom, stateTo));
            }
            return transforms;
        }

        private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, PropertyAndValue<T> pav) {
            return state.setValue(pav.property(), pav.value());
        }

        public record PropertyAndValue<T extends Comparable<T>>(Property<T> property, T value) {
            @SuppressWarnings("unchecked")
            static <T extends Comparable<T>> PropertyAndValue<?> of(StateDefinition<Block, BlockState> definition, String string) {
                String[] split = string.split("=", 2);
                Property<T> prop = (Property<T>) definition.getProperty(split[0]);
                if (prop == null)
                    throw new NullPointerException("Property %s doesn't belong to %s".formatted(split[0], definition));
                T value = prop.getValue(split[1]).orElseThrow();
                return new PropertyAndValue<>(prop, value);
            }

            boolean match(BlockState state) {
                return state.getValue(property) == value;
            }
        }

        public static class PropertiesAndValues extends ArrayList<PropertyAndValue<?>> {
            public static PropertiesAndValues of(StateDefinition<Block, BlockState> definition, String string) {
                PropertiesAndValues result = new PropertiesAndValues();
                for (String s : string.split(","))
                    result.add(PropertyAndValue.of(definition, s));
                return result;
            }

            public boolean match(BlockState state) {
                for (PropertyAndValue<?> pav : this)
                    if (!pav.match(state))
                        return false;
                return true;
            }
        }
    }
}
