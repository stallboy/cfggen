@import configgen.genjava.code.GenCfg
@param GenCfg cfg
package ${cfg.codeTopPkg()};

@if(cfg.jspecify())
@SuppressWarnings("NotNullFieldNotInitialized")
@endif
public abstract class ConfigExtension {
    private ConfigMgr _mgr;

    protected final ConfigMgr _mgr() {
        return _mgr;
    }

    protected void onConfigReady() {}

    final void _bind(ConfigMgr value) {
        @if(cfg.jspecify())
        //noinspection ConstantValue
        @endif
        if (_mgr != null) {
            throw new IllegalStateException("Extension is already bound");
        }
        _mgr = value;
    }
}
