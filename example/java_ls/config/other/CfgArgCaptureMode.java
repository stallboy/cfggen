package config.other;

public enum CfgArgCaptureMode {
    SNAPSHOT("Snapshot"),
    DYNAMIC("Dynamic");

    private final String value;
    private volatile config.other.CfgArgCaptureMode_Detail ref;

    CfgArgCaptureMode(String value) {
        this.value = value;
    }

    public static final java.util.Map<String, CfgArgCaptureMode> map = new java.util.HashMap<>();

    static {
        for(CfgArgCaptureMode e : CfgArgCaptureMode.values()) {
            map.put(e.value, e);
        }
    }

    public static CfgArgCaptureMode get(String value) {
        return map.get(value);
    }

    public String getName() {
        return value;
    }

    public int getId() {
        return ref.getId();
    }

    public String getComment() {
        return ref.getComment();
    }

    public config.other.CfgArgCaptureMode_Detail ref() {
        return ref;
    }

    void setRef(config.ConfigMgr mgr) {
        ref = mgr.other_ArgCaptureMode_All.get(value);
        configgen.genjava.LoadValueErrs.requireNonNull(ref, "other.ArgCaptureMode.setRef", value);
    }

    public static void setAllRefs(config.ConfigMgr mgr) {
        for(CfgArgCaptureMode e : CfgArgCaptureMode.values()) {
            e.setRef(mgr);
        }
    }
}
