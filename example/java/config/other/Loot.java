package config.other;

public class Loot {
    private int lootid;
    private String ename;
    private String name;
    private java.util.List<Integer> chanceList;
    private java.util.List<config.other.Lootitem> ListRefLootid;

    private Loot() {
    }

    public static Loot _create(configgen.genjava.ConfigInput input) {
        Loot self = new Loot();
        self.lootid = input.readInt();
        self.ename = input.readStr();
        self.name = input.readStr();
        self.chanceList = new java.util.ArrayList<>();
        for (int c = input.readInt(); c > 0; c--) {
            self.chanceList.add(input.readInt());
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
    public String getName() {
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

    @Override
    public String toString() {
        return "(" + lootid + "," + ename + "," + name + "," + chanceList + ")";
    }

    public void _resolve(config.ConfigMgr mgr) {
        ListRefLootid = new java.util.ArrayList<>();
        mgr.other_lootitem_All.values().forEach( v -> {
            if (v.getLootid() == lootid)
                ListRefLootid.add(v);
        });
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
