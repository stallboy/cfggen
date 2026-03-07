package config.other;

public class ArgCaptureMode_Detail {
    private String name;
    private String comment;

    private ArgCaptureMode_Detail() {
    }

    public static ArgCaptureMode_Detail _create(configgen.genjava.ConfigInput input) {
        ArgCaptureMode_Detail self = new ArgCaptureMode_Detail();
        self.name = input.readStringInPool();
        self.comment = input.readStringInPool();
        return self;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return "(" + name + "," + comment + ")";
    }

    public static ArgCaptureMode_Detail get(String name) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherArgCaptureMode(name);
    }

    public static java.util.Collection<ArgCaptureMode_Detail> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherArgCaptureMode();
    }

    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                ArgCaptureMode_Detail self = ArgCaptureMode_Detail._create(input);
                mgr.other_ArgCaptureMode_All.put(self.name, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            // no resolve
        }

    }

}
