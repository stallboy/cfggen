package config.other;

import configgen.genjava.LoadValueErrs;

// 来自：other/sign_in.csv
public class SignIn {
    private int id;
    private java.util.Map<Integer, Integer> item2countMap;
    private java.util.Map<Integer, Integer> vipitem2vipcountMap;
    private int viplevel;
    private String iconFile;
    private java.util.Map<Integer, config.other.Loot> RefVipitem2vipcountMap;

    private SignIn() {
    }

    SignIn(SignInBuilder b) {
        this.id = b.id;
        this.item2countMap = b.item2countMap;
        this.vipitem2vipcountMap = b.vipitem2vipcountMap;
        this.viplevel = b.viplevel;
        this.iconFile = b.iconFile;
    }

    public static SignIn _create(configgen.genjava.ConfigInput input) {
        SignIn self = new SignIn();
        self.id = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.item2countMap = java.util.Collections.emptyMap();
            } else {
                self.item2countMap = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    var key = input.readInt();
                    var value = input.readInt();
                    if (self.item2countMap.putIfAbsent(key, value) != null) input.errors().duplicateKey("other.sign_in.item2countMap", key);
                }
                self.item2countMap = java.util.Collections.unmodifiableMap(self.item2countMap);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.vipitem2vipcountMap = java.util.Collections.emptyMap();
            } else {
                self.vipitem2vipcountMap = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    var key = input.readInt();
                    var value = input.readInt();
                    if (self.vipitem2vipcountMap.putIfAbsent(key, value) != null) input.errors().duplicateKey("other.sign_in.vipitem2vipcountMap", key);
                }
                self.vipitem2vipcountMap = java.util.Collections.unmodifiableMap(self.vipitem2vipcountMap);
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

    public java.util.Map<Integer, config.other.Loot> refVipitem2vipcountMap() {
        return RefVipitem2vipcountMap;
    }

    @Override
    public String toString() {
        return "(" + id + "," + item2countMap + "," + vipitem2vipcountMap + "," + viplevel + "," + iconFile + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        if (this.vipitem2vipcountMap.isEmpty()) {
            this.RefVipitem2vipcountMap = java.util.Collections.emptyMap();
        } else {
            this.RefVipitem2vipcountMap = new java.util.LinkedHashMap<>(this.vipitem2vipcountMap.size());
            for (java.util.Map.Entry<Integer, Integer> e : this.vipitem2vipcountMap.entrySet()) {
                var ref = mgr.getOtherLoot(e.getValue());
                if (ref == null) errors.missingReference("other.sign_in", "vipitem2vipcountMap", "other.loot", e.getValue());
                this.RefVipitem2vipcountMap.put(e.getKey(), ref);
            }
        }
        this.RefVipitem2vipcountMap = java.util.Collections.unmodifiableMap(this.RefVipitem2vipcountMap);

    }

    public static SignIn get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherSignIn(id);
    }

    public static java.util.Collection<SignIn> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherSignIn();
    }


}
