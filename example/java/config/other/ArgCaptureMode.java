package config.other;

import configgen.genjava.LoadValueErrs;

public enum ArgCaptureMode {
    SNAPSHOT("Snapshot", 1),
    DYNAMIC("Dynamic", 2);

    private final String value;


    ArgCaptureMode(String value, int fixedId) {
        this.value = value;
        this.fixedId = fixedId;
    }

    private final int fixedId;

    public static final java.util.Map<String, ArgCaptureMode> map;

    static {
        var symbols = new java.util.LinkedHashMap<String, ArgCaptureMode>();
        for(ArgCaptureMode e : ArgCaptureMode.values()) {
            symbols.put(e.value, e);
        }
        map = java.util.Collections.unmodifiableMap(symbols);
    }

    public static ArgCaptureMode get(String value) {
        return map.get(value);
    }

    public String getName() {
        return value;
    }

    public int getId() {
        return ref().getId();
    }

    public String getComment() {
        return ref().getComment();
    }

    public config.other.ArgCaptureMode_Detail ref() {
        return ref(config.ConfigMgr.getMgr());
    }

    public config.other.ArgCaptureMode_Detail ref(config.ConfigMgr mgr) {
        return mgr._get_other_ArgCaptureMode_Refs(ordinal());
    }

    public static void setAllRefs(config.ConfigMgr mgr, LoadValueErrs errors) {
        mgr._set_other_ArgCaptureMode_Refs(new config.other.ArgCaptureMode_Detail[] {
            mgr.getOtherArgCaptureMode(SNAPSHOT.value),
            mgr.getOtherArgCaptureMode(DYNAMIC.value),
        });
        for (var row : mgr._get_other_ArgCaptureMode_All().values()) {
            var key = row.getName();
            String symbol = row.getName();
            ArgCaptureMode expected = get(symbol);
            if (expected == null) {
                errors.unknownSymbol("other.ArgCaptureMode", symbol, key, mgr.explain("other.ArgCaptureMode", key));
                continue;
            }
            if (row.getId() != expected.fixedId) {
                errors.changedSymbolId("other.ArgCaptureMode", symbol, expected.fixedId, row.getId(), mgr.explain("other.ArgCaptureMode", key));
            }
        }
        for (ArgCaptureMode e : map.values()) {
            if (e.ref(mgr) == null) {
                errors.missingSymbol("other.ArgCaptureMode", e.value, e.value);
            }
        }
    }

}
