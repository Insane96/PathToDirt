package insane96mcp.pathtodirt;

import com.mojang.logging.LogUtils;
import insane96mcp.insanelib.setup.ILModConfig;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(PathToDirt.MOD_ID)
public class PathToDirt {
    public static final String MOD_ID = "pathtodirt";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static ILModConfig CONFIG;

    public PathToDirt(IEventBus modEventBus, ModContainer modContainer) {
        CONFIG = new ILModConfig(id("main"), "Single Module", ModConfig.Type.COMMON, modEventBus, PathToDirt.class.getClassLoader());
        modContainer.registerConfig(ModConfig.Type.COMMON, CONFIG.spec);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
