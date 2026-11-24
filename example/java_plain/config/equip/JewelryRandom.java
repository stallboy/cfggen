package config.equip;

import configgen.genjava.LoadValueErrs;

// struct 作为key，有些生成代码是不支持的
// 来自：equip/jewelry_random.csv
public class JewelryRandom {
    private final config.LevelRank lvlRank;
    private final config.Range attackRange;
    private final java.util.List<config.Range> otherRange;
    private final java.util.List<config.equip.TestPackBean> testPack;

    private JewelryRandom(configgen.genjava.ConfigInput input) {
        this.lvlRank = config.LevelRank._create(input);
        this.attackRange = config.Range._create(input);
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.otherRange = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<config.Range>(c);
                for (; c > 0; c--) {
                    items.add(config.Range._create(input));
                }
                this.otherRange = java.util.Collections.unmodifiableList(items);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.testPack = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<config.equip.TestPackBean>(c);
                for (; c > 0; c--) {
                    items.add(config.equip.TestPackBean._create(input));
                }
                this.testPack = java.util.Collections.unmodifiableList(items);
            }
        }
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

    public static JewelryRandom _create(configgen.genjava.ConfigInput input) {
        return new JewelryRandom(input);
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
