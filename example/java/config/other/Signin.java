package config.other;

public class Signin {
    private int id;
    private java.util.Map<Integer, Integer> item2countMap;
    private java.util.Map<Integer, Integer> vipitem2vipcountMap;
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

    public static Signin get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherSignin(id);
    }

    public static class IdViplevelKey {
        private final int id;
        private final int viplevel;

        public IdViplevelKey(int id, int viplevel) {
            this.id = id;
            this.viplevel = viplevel;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, viplevel);
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
        return mgr.getOtherSigninByIdViplevel(id, viplevel);
    }

    public static java.util.Collection<Signin> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherSignin();
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
            // no resolve
        }

    }

}
