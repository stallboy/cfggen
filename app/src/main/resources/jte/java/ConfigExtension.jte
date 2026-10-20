@param String pkg
package ${pkg};

/** Handwritten javaBase classes extend this type. Initialization is driven by the loader. */
public abstract class ConfigExtension {
    private ConfigMgr config;

    protected final ConfigMgr config() { return config; }

    protected void onConfigReady() {}

    final void _bind(ConfigMgr value) {
        if (config != null) {
            throw new IllegalStateException("Extension is already bound");
        }
        config = value;
    }
}
