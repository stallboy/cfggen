package config.equip;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

// struct 作为key，有些生成代码是不支持的
// 来自：equip/jewelry_random.csv
public class CfgJewelry_random {
    private final config.CfgLevelRank lvlRank;
    private final config.CfgRange attackRange;
    private final java.util.List<config.CfgRange> otherRange;
    private final java.util.List<config.equip.CfgTestPackBean> testPack;

    private CfgJewelry_random(configgen.genjava.ConfigInput input) {
        this.lvlRank = config.CfgLevelRank._create(input);
        this.attackRange = config.CfgRange._create(input);
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.otherRange = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<config.CfgRange>(c);
                for (; c > 0; c--) {
                    items.add(config.CfgRange._create(input));
                }
                this.otherRange = java.util.Collections.unmodifiableList(items);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.testPack = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<config.equip.CfgTestPackBean>(c);
                for (; c > 0; c--) {
                    items.add(config.equip.CfgTestPackBean._create(input));
                }
                this.testPack = java.util.Collections.unmodifiableList(items);
            }
        }
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

    public static CfgJewelry_random _create(configgen.genjava.ConfigInput input) {
        return new CfgJewelry_random(input);
    }

    public static @Nullable CfgJewelry_random get(config.CfgLevelRank lvlRank) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getEquipJewelry_random(lvlRank);
    }

    public static java.util.Collection<CfgJewelry_random> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allEquipJewelry_random();
    }


}
