@param String pkg
package ${pkg};

public interface ConfigLoader {

    void createAll(ConfigMgr mgr, configgen.genjava.ConfigInput input);

    void resolveAll(ConfigMgr mgr);

}
