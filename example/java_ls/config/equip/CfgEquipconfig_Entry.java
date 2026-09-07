package config.equip;

// 来自：equip/equipconfig.csv
public class CfgEquipconfig_Entry {
    public static final CfgEquipconfig_Entry INSTANCE = new CfgEquipconfig_Entry("Instance");
    public static final CfgEquipconfig_Entry INSTANCE2 = new CfgEquipconfig_Entry("Instance2");

    private final String value;
    private volatile config.equip.CfgEquipconfig ref;

    CfgEquipconfig_Entry(String value) {
        this.value = value;
    }

    public config.equip.CfgEquipconfig ref() {
        return ref;
    }

    void setRef(config.ConfigMgr mgr) {
        ref = mgr.equip_equipconfig_All.get(value);
        configgen.genjava.LoadValueErrs.requireNonNull(ref, "equip.equipconfig.setRef", value);
    }

    public static void setAllRefs(config.ConfigMgr mgr) {
        INSTANCE.setRef(mgr);
        INSTANCE2.setRef(mgr);
    }
}
