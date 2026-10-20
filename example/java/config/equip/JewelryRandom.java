package config.equip;

import configgen.genjava.LoadValueErrs;

// struct 作为key，有些生成代码是不支持的
// 来自：equip/jewelry_random.csv
public class JewelryRandom {
    private config.LevelRank lvlRank;
    private config.Range attackRange;
    private java.util.List<config.Range> otherRange;
    private java.util.List<config.equip.TestPackBean> testPack;
    private JewelryRandom() {
    }

    public static JewelryRandom _create(configgen.genjava.ConfigInput input) {
        JewelryRandom self = new JewelryRandom();
        self.lvlRank = config.LevelRank._create(input);
        self.attackRange = config.Range._create(input);
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.otherRange = java.util.Collections.emptyList();
            } else {
                self.otherRange = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.otherRange.add(config.Range._create(input));
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
                    self.testPack.add(config.equip.TestPackBean._create(input));
                }
                self.testPack = java.util.Collections.unmodifiableList(self.testPack);
            }
        }
        return self;
    }

    /**
     * 等级
     */
    public config.LevelRank getLvlRank() {
        return lvlRank;
    }

    /**
     * 最小攻击力
     */
    public config.Range getAttackRange() {
        return attackRange;
    }

    /**
     * 最小防御力
     */
    public java.util.List<config.Range> getOtherRange() {
        return otherRange;
    }

    /**
     * 测试pack
     */
    public java.util.List<config.equip.TestPackBean> getTestPack() {
        return testPack;
    }

    @Override
    public String toString() {
        return "(" + lvlRank + "," + attackRange + "," + otherRange + "," + testPack + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.lvlRank._resolve(mgr, errors);
    }

    public static JewelryRandom get(config.LevelRank lvlRank) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getEquipJewelryRandom(lvlRank);
    }

    public static java.util.Collection<JewelryRandom> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allEquipJewelryRandom();
    }


}
