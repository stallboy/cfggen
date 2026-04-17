package config.other;

public enum ArgCaptureMode {
    SNAPSHOT("Snapshot"),
    DYNAMIC("Dynamic");

    private final String value;
    private volatile config.other.ArgCaptureMode_Detail ref;

    ArgCaptureMode(String value) {
        this.value = value;
    }

    private static final java.util.Map<String, ArgCaptureMode> map = new java.util.HashMap<>();

    static {
        for(ArgCaptureMode e : ArgCaptureMode.values()) {
            map.put(e.value, e);
        }
    }

    public static ArgCaptureMode get(String value) {
        return map.get(value);
    }

    public String getName() {
        return value;
    }

    public int getId() {
        return ref.getId();
    }

    public config.Text getComment() {
        return ref.getComment();
    }

    public config.other.ArgCaptureMode_Detail ref() {
        return ref;
    }

    void setRef(config.ConfigMgr mgr) {
        ref = mgr.other_ArgCaptureMode_All.get(value);
        java.util.Objects.requireNonNull(ref);
    }

    public static void setAllRefs(config.ConfigMgr mgr) {
        for(ArgCaptureMode e : ArgCaptureMode.values()) {
            e.setRef(mgr);
        }
    }
}
