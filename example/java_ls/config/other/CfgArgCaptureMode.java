package config.other;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

public enum CfgArgCaptureMode {
    SNAPSHOT("Snapshot", 1),
    DYNAMIC("Dynamic", 2);

    private final String value;


    CfgArgCaptureMode(String value, int fixedId) {
        this.value = value;
        this.fixedId = fixedId;
    }

    private final int fixedId;

    public static final java.util.Map<String, CfgArgCaptureMode> map;

    static {
        var symbols = new java.util.LinkedHashMap<String, CfgArgCaptureMode>();
        for(CfgArgCaptureMode e : CfgArgCaptureMode.values()) {
            symbols.put(e.value, e);
        }
        map = java.util.Collections.unmodifiableMap(symbols);
    }

    public static @Nullable CfgArgCaptureMode get(String value) {
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

    public config.other.CfgArgCaptureMode_Detail ref() {
        return ref(config.ConfigMgr.getMgr());
    }

    public config.other.CfgArgCaptureMode_Detail ref(config.ConfigMgr mgr) {
        return mgr._get_other_ArgCaptureMode_Refs(ordinal());
    }

    public static void setAllRefs(config.ConfigMgr mgr, LoadValueErrs errors) {
        for (var row : mgr._get_other_ArgCaptureMode_All().values()) {
            var key = row.getName();
            String symbol = row.getName();
            CfgArgCaptureMode expected = get(symbol);
            if (expected == null) {
                errors.unknownSymbol("other.ArgCaptureMode", symbol, key, mgr.explain("other.ArgCaptureMode", key));
                continue;
            }
            if (row.getId() != expected.fixedId) {
                errors.changedSymbolId("other.ArgCaptureMode", symbol, expected.fixedId, row.getId(), mgr.explain("other.ArgCaptureMode", key));
            }
        }
        var refs = new config.other.CfgArgCaptureMode_Detail[2];
        boolean complete = true;
        for (CfgArgCaptureMode e : map.values()) {
            var row = mgr.getOtherArgCaptureMode(e.value);
            if (row == null) {
                errors.missingSymbol("other.ArgCaptureMode", e.value, e.value);
                complete = false;
            } else {
                refs[e.ordinal()] = row;
            }
        }
        if (complete) {
            mgr._set_other_ArgCaptureMode_Refs(refs);
        }
    }

}
