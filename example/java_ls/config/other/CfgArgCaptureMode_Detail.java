package config.other;

public class CfgArgCaptureMode_Detail {
    private String name;
    private int id;
    private String comment;

    private CfgArgCaptureMode_Detail() {
    }

    public static CfgArgCaptureMode_Detail _create(configgen.genjava.ConfigInput input) {
        CfgArgCaptureMode_Detail self = new CfgArgCaptureMode_Detail();
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

    public static CfgArgCaptureMode_Detail get(String name) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherArgCaptureMode(name);
    }

    public static CfgArgCaptureMode_Detail getById(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherArgCaptureModeById(id);
    }

    public static java.util.Collection<CfgArgCaptureMode_Detail> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherArgCaptureMode();
    }
    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            int c = input.readInt();
            mgr.other_ArgCaptureMode_All = new java.util.LinkedHashMap<>(c);
            mgr.other_ArgCaptureMode_IdMap = new java.util.LinkedHashMap<>(c);
            for (; c > 0; c--) {
                CfgArgCaptureMode_Detail self = CfgArgCaptureMode_Detail._create(input);
                mgr.other_ArgCaptureMode_All.put(self.name, self);
                mgr.other_ArgCaptureMode_IdMap.put(self.id, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            // no resolve
        }

    }

}
