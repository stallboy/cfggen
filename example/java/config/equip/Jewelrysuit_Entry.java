package config.equip;

public class Jewelrysuit_Entry {
    public static final Jewelrysuit_Entry SPECIALSUIT = new Jewelrysuit_Entry("SpecialSuit", 4);

    private final String name;
    private final int value;
    private volatile config.equip.Jewelrysuit ref;

    Jewelrysuit_Entry(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public config.equip.Jewelrysuit ref() {
        return ref;
    }

    void setRef(config.ConfigMgr mgr) {
        ref = mgr.equip_jewelrysuit_All.get(value);
        java.util.Objects.requireNonNull(ref);
    }

    public static void setAllRefs(config.ConfigMgr mgr) {
        SPECIALSUIT.setRef(mgr);
    }
}
