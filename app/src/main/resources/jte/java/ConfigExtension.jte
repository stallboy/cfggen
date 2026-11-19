@import configgen.genjava.code.GenCfg
@param GenCfg cfg
package ${cfg.codeTopPkg()};

@if(cfg.jspecify())
import org.jspecify.annotations.Nullable;

@endif
/** Handwritten javaBase classes extend this type. Initialization is driven by the loader. */
public abstract class ConfigExtension {
    private ${cfg.nullable("ConfigMgr")} config;

    protected final ${cfg.nullable("ConfigMgr")} config() { return config; }

    protected void onConfigReady() {}

    final void _bind(ConfigMgr value) {
        if (config != null) {
            throw new IllegalStateException("Extension is already bound");
        }
        config = value;
    }
}
