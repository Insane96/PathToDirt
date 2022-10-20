package insane96mcp.pathtodirt.module;

import insane96mcp.insanelib.base.Module;
import insane96mcp.pathtodirt.PathToDirt;

public class Modules {
    public static Module BaseModule;

    public static void init() {
        BaseModule = Module.Builder.create(PathToDirt.RESOURCE_PREFIX + "base", "Path to Dirt")
            .canBeDisabled(false)
            .build();
    }
}
