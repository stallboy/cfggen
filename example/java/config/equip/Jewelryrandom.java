package config.equip;

public class Jewelryrandom {
    private config.LevelRank lvlRank;
    private config.Range attackRange;
    private java.util.List<config.Range> otherRange;
    private java.util.List<config.equip.TestPackBean> testPack;

    private Jewelryrandom() {
    }

    public static Jewelryrandom _create(configgen.genjava.ConfigInput input) {
        Jewelryrandom self = new Jewelryrandom();
        self.lvlRank = config.LevelRank._create(input);
        self.attackRange = config.Range._create(input);
        {
            int c = input.readInt();
            if (c == 0) {
                self.otherRange = java.util.Collections.emptyList();
            } else {
                self.otherRange = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.otherRange.add(config.Range._create(input));
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
                    self.testPack.add(config.equip.TestPackBean._create(input));
                }
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

    public void _resolve(config.ConfigMgr mgr) {
        lvlRank._resolve(mgr);
    }

    public static Jewelryrandom get(config.LevelRank lvlRank) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getEquipJewelryrandom(lvlRank);
    }

    public static java.util.Collection<Jewelryrandom> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allEquipJewelryrandom();
    }

    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                Jewelryrandom self = Jewelryrandom._create(input);
                mgr.equip_jewelryrandom_All.put(self.lvlRank, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            for (Jewelryrandom e : mgr.equip_jewelryrandom_All.values()) {
                e._resolve(mgr);
            }
        }

    }

}
