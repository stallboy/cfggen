package config;

public interface ConfigLoader {

    void createAll(ConfigMgr mgr, configgen.genjava.ConfigInput input);

    void resolveAll(ConfigMgr mgr);

}
