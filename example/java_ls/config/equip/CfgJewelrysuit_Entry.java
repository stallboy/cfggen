package config.equip;

// 来自：equip/jewelrysuit.csv
public class CfgJewelrysuit_Entry {
    public static final CfgJewelrysuit_Entry SPECIALSUIT = new CfgJewelrysuit_Entry("SpecialSuit", 4);

    private final String name;
    private final int value;
    private volatile config.equip.CfgJewelrysuit ref;

    CfgJewelrysuit_Entry(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public config.equip.CfgJewelrysuit ref() {
        return ref;
    }

    void setRef(config.ConfigMgr mgr) {
        ref = mgr.equip_jewelrysuit_All.get(value);
        configgen.genjava.LoadValueErrs.requireNonNull(ref, "equip.jewelrysuit.setRef", value);
    }

    public static void setAllRefs(config.ConfigMgr mgr) {
        SPECIALSUIT.setRef(mgr);
    }
}
