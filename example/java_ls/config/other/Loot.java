package config.other;

public class Loot {
    private int lootid;
    private String ename;
    private config.Text name;
    private java.util.List<Integer> chanceList;
    private java.util.List<config.other.Lootitem> ListRefLootid;
    private java.util.List<config.other.Lootitem> ListRefAnotherWay;

    private Loot() {
    }

    public static Loot _create(configgen.genjava.ConfigInput input) {
        Loot self = new Loot();
        self.lootid = input.readInt();
        self.ename = input.readStringInPool();
        self.name = config.Text._create(input);
        {
            int c = input.readInt();
            if (c == 0) {
                self.chanceList = java.util.Collections.emptyList();
            } else {
                self.chanceList = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.chanceList.add(input.readInt());
                }
            }
        }
        return self;
    }

    /**
     * 序号
     */
    public int getLootid() {
        return lootid;
    }

    public String getEname() {
        return ename;
    }

    /**
     * 名字
     */
    public config.Text getName() {
        return name;
    }

    /**
     * 掉落0件物品的概率
     */
    public java.util.List<Integer> getChanceList() {
        return chanceList;
    }

    public java.util.List<config.other.Lootitem> listRefLootid() {
        return ListRefLootid;
    }

    public java.util.List<config.other.Lootitem> listRefAnotherWay() {
        return ListRefAnotherWay;
    }

    @Override
    public String toString() {
        return "(" + lootid + "," + ename + "," + name + "," + chanceList + ")";
    }

    public void _resolveDirect(config.ConfigMgr mgr) {
        ListRefLootid = new java.util.ArrayList<>();
        for (config.other.Lootitem v : mgr.other_lootitem_All.values()) {
            if (v.getLootid() == lootid)
                ListRefLootid.add(v);
        }
        ListRefLootid = ListRefLootid.isEmpty() ? java.util.Collections.emptyList() : new java.util.ArrayList<>(ListRefLootid);
        ListRefAnotherWay = new java.util.ArrayList<>();
        for (config.other.Lootitem v : mgr.other_lootitem_All.values()) {
            if (v.getLootid() == lootid)
                ListRefAnotherWay.add(v);
        }
        ListRefAnotherWay = ListRefAnotherWay.isEmpty() ? java.util.Collections.emptyList() : new java.util.ArrayList<>(ListRefAnotherWay);
    }

    public void _resolve(config.ConfigMgr mgr) {
        _resolveDirect(mgr);
    }

    public static Loot get(int lootid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherLoot(lootid);
    }

    public static java.util.Collection<Loot> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherLoot();
    }

    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                Loot self = Loot._create(input);
                mgr.other_loot_All.put(self.lootid, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            for (Loot e : mgr.other_loot_All.values()) {
                e._resolve(mgr);
            }
        }

    }

}
