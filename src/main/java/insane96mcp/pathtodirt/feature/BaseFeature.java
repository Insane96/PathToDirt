package insane96mcp.pathtodirt.feature;

import insane96mcp.insanelib.core.feature.Feature;
import insane96mcp.insanelib.core.feature.LoadFeature;
import insane96mcp.insanelib.core.feature.config.Blacklist;
import insane96mcp.insanelib.core.feature.config.Config;
import insane96mcp.pathtodirt.PathToDirt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@LoadFeature(name = "Path to Dirt", canBeDisabled = false)
public class BaseFeature extends Feature {
    @Config(name = "Transformations List", description = """
            Write here a list of custom overrides when right clicking a block.
            Format: blockFrom>blockTo
            Both sides support optional block state properties: blockFrom[prop=val,prop2=val2]>blockTo[prop=val]
            blockFrom can be a tag (prefixed with #), but tags cannot be combined with state properties.
            If no state properties are specified on the source side, all states of that block match.
            If no state properties are specified on the target side, the default state is used.
            E.g. minecraft:farmland>minecraft:dirt will make farmland transform to dirt when right-clicked with a shovel.
            E.g. minecraft:farmland[mositure=7]>minecraft:dirt will only match wet farmland.""")
    public static List<String> transformListConfig = List.of("minecraft:dirt_path>minecraft:dirt", "minecraft:farmland>minecraft:dirt");

    public static List<Transform> transformList = new ArrayList<>();

    @Config(name = "Item Blacklist", description = "Items and tags that should not perform the block transformation. By default any item that uses ItemAbilities.SHOVEL_FLATTEN will work. Note that items in this list will only be prevented from executing the transformations above and not the default Dirt to Path transformation.")
    public static Blacklist<Item> itemBlacklist = new Blacklist<>(Blacklist.parseStringList(List.of(
            "quark:pickarang",
            "quark:netherite_pickarang"
    ), Registries.ITEM), false, Registries.ITEM);

    @Override
    public void readConfig(final ModConfigEvent event) {
        super.readConfig(event);
        transformList = Transform.parseList(transformListConfig);
    }

    @SubscribeEvent
    public void onRightClick(BlockEvent.BlockToolModificationEvent event) {
        if (!this.isEnabled()
                || event.getLevel().isClientSide()
                || event.isSimulated()
                || event.getItemAbility() != ItemAbilities.SHOVEL_FLATTEN
                || event.getState().getBlock().getToolModifiedState(event.getState(), event.getContext(), event.getItemAbility(), true) != null
                || !event.getContext().getLevel().isEmptyBlock(event.getPos().above())
                || itemBlacklist.isBlackOrNotWhiteListed(event.getHeldItemStack().getItem()))
            return;

        for (Transform transform : transformList) {
            if (!transform.matches(event.getState()))
                continue;

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
                    PathToDirt.LOGGER.warn("Invalid line \"{}\". Format must be blockFrom>blockTo", line);
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
                    PathToDirt.LOGGER.warn("Tags are not allowed on the target side of \"{}\"", line);
                    continue;
                }
                ResourceLocation toLocation = ResourceLocation.tryParse(toId);
                if (toLocation == null) {
                    PathToDirt.LOGGER.warn("{} is not a valid resource location", toId);
                    continue;
                }
                Block toBlock = BuiltInRegistries.BLOCK.getOptional(toLocation).orElse(null);
                if (toBlock == null) {
                    PathToDirt.LOGGER.warn("{} block doesn't exist", toId);
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
                        PathToDirt.LOGGER.warn("Failed to parse target state properties for \"{}\": {}", line, e.getMessage());
                        continue;
                    }
                }

                // Resolve source side
                Block blockFrom = null;
                TagKey<Block> tagFrom = null;
                List<BlockState> statesFrom = new ArrayList<>();

                if (fromId.startsWith("#")) {
                    if (fromProps != null) {
                        PathToDirt.LOGGER.warn("Tags cannot be combined with state properties in \"{}\"", line);
                        continue;
                    }
                    ResourceLocation tagLocation = ResourceLocation.tryParse(fromId.substring(1));
                    if (tagLocation == null) {
                        PathToDirt.LOGGER.warn("{} is not a valid tag resource location", fromId);
                        continue;
                    }
                    tagFrom = TagKey.create(Registries.BLOCK, tagLocation);
                } else {
                    ResourceLocation fromLocation = ResourceLocation.tryParse(fromId);
                    if (fromLocation == null) {
                        PathToDirt.LOGGER.warn("{} is not a valid resource location", fromId);
                        continue;
                    }
                    blockFrom = BuiltInRegistries.BLOCK.getOptional(fromLocation).orElse(null);
                    if (blockFrom == null) {
                        PathToDirt.LOGGER.warn("{} block doesn't exist", fromId);
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
                            PathToDirt.LOGGER.warn("Failed to parse source state properties for \"{}\": {}", line, e.getMessage());
                            continue;
                        }
                        if (statesFrom.isEmpty()) {
                            PathToDirt.LOGGER.warn("No block states matched the properties for \"{}\"", line);
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
