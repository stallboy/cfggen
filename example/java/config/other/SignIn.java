package config.other;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

// 来自：other/sign_in.csv
public class SignIn {
    private final int id;
    private final java.util.Map<Integer, Integer> item2countMap;
    private final java.util.Map<Integer, Integer> vipitem2vipcountMap;
    private final int viplevel;
    private final String iconFile;
    private java.util.@Nullable Map<Integer, config.other.@Nullable Loot> RefVipitem2vipcountMap;

    SignIn(SignInBuilder b) {
        this.id = b.id;
        this.item2countMap = b.item2countMap;
        this.vipitem2vipcountMap = b.vipitem2vipcountMap;
        this.viplevel = b.viplevel;
        this.iconFile = b.iconFile;
    }

    private SignIn(configgen.genjava.ConfigInput input) {
        this.id = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.item2countMap = java.util.Collections.emptyMap();
            } else {
                var entries = new java.util.LinkedHashMap<Integer, Integer>(c);
                for (; c > 0; c--) {
                    var key = input.readInt();
                    var value = input.readInt();
                    if (entries.putIfAbsent(key, value) != null) {
                        input.errors().duplicateKey("other.sign_in.item2countMap", key);
                    }
                }
                this.item2countMap = java.util.Collections.unmodifiableMap(entries);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.vipitem2vipcountMap = java.util.Collections.emptyMap();
            } else {
                var entries = new java.util.LinkedHashMap<Integer, Integer>(c);
                for (; c > 0; c--) {
                    var key = input.readInt();
                    var value = input.readInt();
                    if (entries.putIfAbsent(key, value) != null) {
                        input.errors().duplicateKey("other.sign_in.vipitem2vipcountMap", key);
                    }
                }
                this.vipitem2vipcountMap = java.util.Collections.unmodifiableMap(entries);
            }
        }
        this.viplevel = input.readInt();
        this.iconFile = input.readStringInPool();
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

    public java.util.@Nullable Map<Integer, config.other.@Nullable Loot> refVipitem2vipcountMap() {
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
                if (ref == null) {
                    errors.missingReference("other.sign_in", "vipitem2vipcountMap", "other.loot", e.getValue());
                }
                this.RefVipitem2vipcountMap.put(e.getKey(), ref);
            }
        }
        this.RefVipitem2vipcountMap = java.util.Collections.unmodifiableMap(this.RefVipitem2vipcountMap);

    }

    public static SignIn _create(configgen.genjava.ConfigInput input) {
        return new SignIn(input);
    }

    public static @Nullable SignIn get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherSignIn(id);
    }

    public static java.util.Collection<SignIn> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherSignIn();
    }


}
