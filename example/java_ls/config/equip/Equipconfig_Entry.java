package config.equip;

public class Equipconfig_Entry {
    public static final Equipconfig_Entry INSTANCE = new Equipconfig_Entry("Instance");
    public static final Equipconfig_Entry INSTANCE2 = new Equipconfig_Entry("Instance2");

    private final String value;
    private volatile config.equip.Equipconfig ref;

    Equipconfig_Entry(String value) {
        this.value = value;
    }

    public config.equip.Equipconfig ref() {
        return ref;
    }

    void setRef(config.ConfigMgr mgr) {
        ref = mgr.equip_equipconfig_All.get(value);
        java.util.Objects.requireNonNull(ref);
    }

    public static void setAllRefs(config.ConfigMgr mgr) {
        INSTANCE.setRef(mgr);
        INSTANCE2.setRef(mgr);
    }
}
