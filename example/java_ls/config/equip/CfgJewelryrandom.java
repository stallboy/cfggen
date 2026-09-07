package config.equip;

// struct 作为key，有些生成代码是不支持的
// 来自：equip/jewelryrandom.csv
public class CfgJewelryrandom {
    private config.CfgLevelRank lvlRank;
    private config.CfgRange attackRange;
    private java.util.List<config.CfgRange> otherRange;
    private java.util.List<config.equip.CfgTestPackBean> testPack;

    private CfgJewelryrandom() {
    }

    public static CfgJewelryrandom _create(configgen.genjava.ConfigInput input) {
        CfgJewelryrandom self = new CfgJewelryrandom();
        self.lvlRank = config.CfgLevelRank._create(input);
        self.attackRange = config.CfgRange._create(input);
        {
            int c = input.readInt();
            if (c == 0) {
                self.otherRange = java.util.Collections.emptyList();
            } else {
                self.otherRange = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.otherRange.add(config.CfgRange._create(input));
                }
            }
        }
        {
            int c = input.readInt();
            if (c == 0) {
                self.testPack = java.util.Collections.emptyList();
            } else {
                self.testPack = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.testPack.add(config.equip.CfgTestPackBean._create(input));
                }
            }
        }
        return self;
    }

    /**
     * 等级
     */
    public config.CfgLevelRank getLvlRank() {
        return lvlRank;
    }

    /**
     * 最小攻击力
     */
    public config.CfgRange getAttackRange() {
        return attackRange;
    }

    /**
     * 最小防御力
     */
    public java.util.List<config.CfgRange> getOtherRange() {
        return otherRange;
    }

    /**
     * 测试pack
     */
    public java.util.List<config.equip.CfgTestPackBean> getTestPack() {
        return testPack;
    }

    @Override
    public String toString() {
        return "(" + lvlRank + "," + attackRange + "," + otherRange + "," + testPack + ")";
    }

    public void _resolve(config.ConfigMgr mgr) {
        lvlRank._resolve(mgr);
    }

    public static CfgJewelryrandom get(config.CfgLevelRank lvlRank) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getEquipJewelryrandom(lvlRank);
    }

    public static java.util.Collection<CfgJewelryrandom> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allEquipJewelryrandom();
    }
    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            int c = input.readInt();
            mgr.equip_jewelryrandom_All = new java.util.LinkedHashMap<>(c);
            for (; c > 0; c--) {
                CfgJewelryrandom self = CfgJewelryrandom._create(input);
                mgr.equip_jewelryrandom_All.put(self.lvlRank, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            for (CfgJewelryrandom e : mgr.equip_jewelryrandom_All.values()) {
                e._resolve(mgr);
            }
        }

    }

}
