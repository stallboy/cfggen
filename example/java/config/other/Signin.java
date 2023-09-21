package config.other;

public class Signin {
    private int id;
    private java.util.Map<Integer, Integer> item2countMap;
    private java.util.Map<Integer, Integer> vipitem2vipcountMap;
    private java.util.Map<Integer, config.other.Loot> RefVipitem2vipcountMap;
    private int viplevel;
    private String iconFile;

    private Signin() {
    }

    Signin(SigninBuilder b) {
        this.id = b.id;
        this.item2countMap = b.item2countMap;
        this.vipitem2vipcountMap = b.vipitem2vipcountMap;
        this.viplevel = b.viplevel;
        this.iconFile = b.iconFile;
    }

    public static Signin _create(configgen.genjava.ConfigInput input) {
        Signin self = new Signin();
        self.id = input.readInt();
        self.item2countMap = new java.util.LinkedHashMap<>();
        for (int c = input.readInt(); c > 0; c--) {
            self.item2countMap.put(input.readInt(), input.readInt());
        }
        self.vipitem2vipcountMap = new java.util.LinkedHashMap<>();
        for (int c = input.readInt(); c > 0; c--) {
            self.vipitem2vipcountMap.put(input.readInt(), input.readInt());
        }
        self.viplevel = input.readInt();
        self.iconFile = input.readStr();
        return self;
    }

    /**
     * 礼包ID
     */
    public int getId() {
        return id;
    }

    /**
     * 普通奖励
     */
    public java.util.Map<Integer, Integer> getItem2countMap() {
        return item2countMap;
    }

    /**
     * vip奖励
     */
    public java.util.Map<Integer, Integer> getVipitem2vipcountMap() {
        return vipitem2vipcountMap;
    }

    public java.util.Map<Integer, config.other.Loot> refVipitem2vipcountMap() {
        return RefVipitem2vipcountMap;
    }

    /**
     * 领取vip奖励的最低等级
     */
    public int getViplevel() {
        return viplevel;
    }

    /**
     * 礼包图标
     */
    public String getIconFile() {
        return iconFile;
    }

    @Override
    public String toString() {
        return "(" + id + "," + item2countMap + "," + vipitem2vipcountMap + "," + viplevel + "," + iconFile + ")";
    }

    public void _resolve(config.ConfigMgr mgr) {
        RefVipitem2vipcountMap = new java.util.LinkedHashMap<>();
        vipitem2vipcountMap.forEach( (k, v) -> {
            config.other.Loot rv = mgr.other_loot_All.get(v);
            java.util.Objects.requireNonNull(rv);
            RefVipitem2vipcountMap.put(k, rv);
        });
    }

    public static Signin get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.other_signin_All.get(id);
    }

    public static class IdViplevelKey {
        private final int id;
        private final int viplevel;

        IdViplevelKey(int id, int viplevel) {
            this.id = id;
            this.viplevel = viplevel;
        }

        @Override
        public int hashCode() {
            return id + viplevel;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof IdViplevelKey))
                return false;
            IdViplevelKey o = (IdViplevelKey) other;
            return id == o.id && viplevel == o.viplevel;
        }
    }

    public static Signin getByIdViplevel(int id, int viplevel) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.other_signin_IdViplevelMap.get(new IdViplevelKey(id, viplevel));
    }

    public static java.util.Collection<Signin> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.other_signin_All.values();
    }

    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                Signin self = Signin._create(input);
                mgr.other_signin_All.put(self.id, self);
                mgr.other_signin_IdViplevelMap.put(new IdViplevelKey(self.id, self.viplevel), self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            for (Signin e : mgr.other_signin_All.values()) {
                e._resolve(mgr);
            }
        }

    }

}
