package config.other;

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
        self.comment = input.readTextInPool();
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
    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            int c = input.readInt();
            mgr.other_ArgCaptureMode_All = new java.util.LinkedHashMap<>(c);
            mgr.other_ArgCaptureMode_IdMap = new java.util.LinkedHashMap<>(c);
            for (; c > 0; c--) {
                ArgCaptureMode_Detail self = ArgCaptureMode_Detail._create(input);
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
