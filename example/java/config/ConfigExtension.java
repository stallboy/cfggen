package config;

import org.jspecify.annotations.Nullable;

/** Handwritten javaBase classes extend this type. Initialization is driven by the loader. */
public abstract class ConfigExtension {
    private @Nullable ConfigMgr config;

    protected final @Nullable ConfigMgr config() { return config; }

    protected void onConfigReady() {}

    final void _bind(ConfigMgr value) {
        if (config != null) {
            throw new IllegalStateException("Extension is already bound");
        }
        config = value;
    }
}
