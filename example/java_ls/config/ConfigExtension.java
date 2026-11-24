package config;

@SuppressWarnings("NotNullFieldNotInitialized")
public abstract class ConfigExtension {
    private ConfigMgr _mgr;

    protected final ConfigMgr _mgr() {
        return _mgr;
    }

    protected void onConfigReady() {}

    final void _bind(ConfigMgr value) {
        //noinspection ConstantValue
        if (_mgr != null) {
            throw new IllegalStateException("Extension is already bound");
        }
        _mgr = value;
    }
}
