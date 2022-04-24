package insane96mcp.pathtodirt.module.base;

import insane96mcp.insanelib.base.Label;
import insane96mcp.insanelib.base.Module;
import insane96mcp.pathtodirt.module.base.feature.BaseFeature;
import insane96mcp.pathtodirt.setup.Config;

@Label(name = "base")
public class BaseModule extends Module {

    public BaseFeature base;

    public BaseModule() {
        super(Config.builder, true, false);
        base = new BaseFeature(this);
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        base.loadConfig();
    }
}
