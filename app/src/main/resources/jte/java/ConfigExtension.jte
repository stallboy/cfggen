@import configgen.genjava.code.GenCfg
@param GenCfg cfg
package ${cfg.codeTopPkg()};

public abstract class ConfigExtension {
    protected void onConfigReady(ConfigMgr _mgr) {}
}
