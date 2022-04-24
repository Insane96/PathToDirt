package insane96mcp.pathtodirt.module;

import insane96mcp.pathtodirt.module.base.BaseModule;

public class Modules {
    public static BaseModule base;

    public static void init() {
        base = new BaseModule();
    }

    public static void loadConfig() {
        base.loadConfig();
    }
}
