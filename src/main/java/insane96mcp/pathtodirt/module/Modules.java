package insane96mcp.pathtodirt.module;

import insane96mcp.insanelib.base.Module;
import insane96mcp.pathtodirt.PathToDirt;
import insane96mcp.pathtodirt.setup.Config;

public class Modules {
    public static Module BaseModule;

    public static void init() {
        BaseModule = Module.Builder.create(Config.builder, PathToDirt.RESOURCE_PREFIX + "base", "Path to Dirt")
            .canBeDisabled(false)
            .build();
    }
}
