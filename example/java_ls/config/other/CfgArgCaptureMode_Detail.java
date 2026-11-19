package config.other;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

public class CfgArgCaptureMode_Detail {
    private final String name;
    private final int id;
    private final String comment;

    private CfgArgCaptureMode_Detail(configgen.genjava.ConfigInput input) {
        this.name = input.readStringInPool();
        this.id = input.readInt();
        this.comment = input.readStringInPool();
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return "(" + name + "," + id + "," + comment + ")";
    }

    public static CfgArgCaptureMode_Detail _create(configgen.genjava.ConfigInput input) {
        return new CfgArgCaptureMode_Detail(input);
    }

    public static @Nullable CfgArgCaptureMode_Detail get(String name) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherArgCaptureMode(name);
    }

    public static @Nullable CfgArgCaptureMode_Detail getById(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherArgCaptureModeById(id);
    }

    public static java.util.Collection<CfgArgCaptureMode_Detail> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherArgCaptureMode();
    }

    public static void _index(config.ConfigMgr mgr, java.util.Map<String, CfgArgCaptureMode_Detail> rows, LoadValueErrs errors) {
        int c = rows.size();
        var other_ArgCaptureMode_IdMap = new java.util.LinkedHashMap<Integer, CfgArgCaptureMode_Detail>(c);
        for (var self : rows.values()) {
            if (other_ArgCaptureMode_IdMap.putIfAbsent(self.id, self) != null) {
                var primaryKey = self.name;
                errors.duplicateUniqueKey("other.ArgCaptureMode", "id", primaryKey, self.id, mgr.explain("other.ArgCaptureMode", primaryKey));
            }
        }
        mgr._set_other_ArgCaptureMode_All(rows);
        mgr._set_other_ArgCaptureMode_IdMap(other_ArgCaptureMode_IdMap);
    }

}
