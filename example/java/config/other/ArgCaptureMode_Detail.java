package config.other;

import configgen.genjava.LoadValueErrs;

public class ArgCaptureMode_Detail {
    private String name;
    private int id;
    private String comment;
    private ArgCaptureMode_Detail() {
    }

    public static ArgCaptureMode_Detail _create(configgen.genjava.ConfigInput input) {
        ArgCaptureMode_Detail self = new ArgCaptureMode_Detail();
        self.name = input.readStringInPool();
        self.id = input.readInt();
        self.comment = input.readStringInPool();
        return self;
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

    public static ArgCaptureMode_Detail get(String name) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherArgCaptureMode(name);
    }

    public static ArgCaptureMode_Detail getById(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherArgCaptureModeById(id);
    }

    public static java.util.Collection<ArgCaptureMode_Detail> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherArgCaptureMode();
    }

    public static void _index(config.ConfigMgr mgr, java.util.Map<String, ArgCaptureMode_Detail> rows, LoadValueErrs errors) {
        int c = rows.size();
        var other_ArgCaptureMode_IdMap = new java.util.LinkedHashMap<Integer, ArgCaptureMode_Detail>(c);
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
