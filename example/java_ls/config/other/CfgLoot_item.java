package config.other;

// 来自：other/loot_item.csv, other/loot_item_1.csv, other/loot_item_2.csv
public class CfgLoot_item {
    private int lootid;
    private int itemid;
    private int chance;
    private int countmin;
    private int countmax;
    private CfgLoot_item() {
    }

    public static CfgLoot_item _create(configgen.genjava.ConfigInput input) {
        CfgLoot_item self = new CfgLoot_item();
        self.lootid = input.readInt();
        self.itemid = input.readInt();
        self.chance = input.readInt();
        self.countmin = input.readInt();
        self.countmax = input.readInt();
        return self;
    }

    /**
     * 掉落id
     */
    public int getLootid() {
        return lootid;
    }

    /**
     * 掉落物品
     */
    public int getItemid() {
        return itemid;
    }

    /**
     * 掉落概率
     */
    public int getChance() {
        return chance;
    }

    /**
     * 数量下限
     */
    public int getCountmin() {
        return countmin;
    }

    /**
     * 数量上限
     */
    public int getCountmax() {
        return countmax;
    }

    @Override
    public String toString() {
        return "(" + lootid + "," + itemid + "," + chance + "," + countmin + "," + countmax + ")";
    }

    public static class LootidItemidKey {
        private final int lootid;
        private final int itemid;

        public LootidItemidKey(int lootid, int itemid) {
            this.lootid = lootid;
            this.itemid = itemid;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(lootid, itemid);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof LootidItemidKey o))
                return false;
            return lootid == o.lootid && itemid == o.itemid;
        }

        @Override
        public String toString() {
            return "(" + lootid + "," + itemid + ")";
        }
    }

    public static CfgLoot_item get(int lootid, int itemid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherLoot_item(lootid, itemid);
    }

    public static java.util.Collection<CfgLoot_item> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherLoot_item();
    }


}
