package config.equip;

import configgen.genjava.LoadValueErrs;

// struct 作为key，有些生成代码是不支持的
// 来自：equip/jewelry_random.csv
public class CfgJewelry_random {
    private config.CfgLevelRank lvlRank;
    private config.CfgRange attackRange;
    private java.util.List<config.CfgRange> otherRange;
    private java.util.List<config.equip.CfgTestPackBean> testPack;
    private CfgJewelry_random() {
    }

    public static CfgJewelry_random _create(configgen.genjava.ConfigInput input) {
        CfgJewelry_random self = new CfgJewelry_random();
        self.lvlRank = config.CfgLevelRank._create(input);
        self.attackRange = config.CfgRange._create(input);
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.otherRange = java.util.Collections.emptyList();
            } else {
                self.otherRange = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.otherRange.add(config.CfgRange._create(input));
                }
                self.otherRange = java.util.Collections.unmodifiableList(self.otherRange);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.testPack = java.util.Collections.emptyList();
            } else {
                self.testPack = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.testPack.add(config.equip.CfgTestPackBean._create(input));
                }
                self.testPack = java.util.Collections.unmodifiableList(self.testPack);
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

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.lvlRank._resolve(mgr, errors);
    }

    public static CfgJewelry_random get(config.CfgLevelRank lvlRank) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getEquipJewelry_random(lvlRank);
    }

    public static java.util.Collection<CfgJewelry_random> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allEquipJewelry_random();
    }


}
