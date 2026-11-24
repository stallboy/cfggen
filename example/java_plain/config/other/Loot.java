package config.other;

import configgen.genjava.LoadValueErrs;

// 来自：other/loot.csv
public class Loot {
    private final int lootid;
    private final String ename;
    private final String name;
    private final java.util.List<Integer> chanceList;
    private java.util.List<config.other.LootItem> ListRefLootid;
    private java.util.List<config.other.LootItem> ListRefAnotherWay;

    private Loot(configgen.genjava.ConfigInput input) {
        this.lootid = input.readInt();
        this.ename = input.readStringInPool();
        this.name = input.readTextInPool();
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.chanceList = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<Integer>(c);
                for (; c > 0; c--) {
                    items.add(input.readInt());
                }
                this.chanceList = java.util.Collections.unmodifiableList(items);
            }
        }
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

    public java.util.List<config.other.LootItem> listRefLootid() {
        return ListRefLootid;
    }

    public java.util.List<config.other.LootItem> listRefAnotherWay() {
        return ListRefAnotherWay;
    }

    @Override
    public String toString() {
        return "(" + lootid + "," + ename + "," + name + "," + chanceList + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.ListRefLootid = new java.util.ArrayList<>();
        for (config.other.LootItem v : mgr._get_other_loot_item_All().values()) {
            if (v.getLootid() == this.lootid) {
                this.ListRefLootid.add(v);
            }
        }
        this.ListRefLootid = java.util.Collections.unmodifiableList(this.ListRefLootid);
        this.ListRefAnotherWay = new java.util.ArrayList<>();
        for (config.other.LootItem v : mgr._get_other_loot_item_All().values()) {
            if (v.getLootid() == this.lootid) {
                this.ListRefAnotherWay.add(v);
            }
        }
        this.ListRefAnotherWay = java.util.Collections.unmodifiableList(this.ListRefAnotherWay);

    }

    public static Loot _create(configgen.genjava.ConfigInput input) {
        return new Loot(input);
    }

    public static Loot get(int lootid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherLoot(lootid);
    }

    public static java.util.Collection<Loot> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherLoot();
    }


}
