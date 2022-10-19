package insane96mcp.pathtodirt;


import insane96mcp.pathtodirt.module.Modules;
import insane96mcp.pathtodirt.setup.Config;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(PathToDirt.MOD_ID)
public class PathToDirt {
    public static final String MOD_ID = "pathtodirt";
    public static final String RESOURCE_PREFIX = MOD_ID + ":";

    public static final Logger LOGGER = LogManager.getLogger();

    public PathToDirt() {
        Modules.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
    }
}
