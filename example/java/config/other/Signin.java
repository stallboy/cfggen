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
        {
            int c = input.readInt();
            if (c == 0) {
                self.item2countMap = java.util.Collections.emptyMap();
            } else {
                self.item2countMap = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    self.item2countMap.put(input.readInt(), input.readInt());
                }
            }
        }
        {
            int c = input.readInt();
            if (c == 0) {
                self.vipitem2vipcountMap = java.util.Collections.emptyMap();
            } else {
                self.vipitem2vipcountMap = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    self.vipitem2vipcountMap.put(input.readInt(), input.readInt());
                }
            }
        }
        self.viplevel = input.readInt();
        self.iconFile = input.readStringInPool();
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
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            // no resolve
        }

    }

}
