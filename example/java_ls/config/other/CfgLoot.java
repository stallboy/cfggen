package config.other;

import configgen.genjava.LoadValueErrs;

// 来自：other/loot.csv
public class CfgLoot {
    private int lootid;
    private String ename;
    private config.Text name;
    private java.util.List<Integer> chanceList;
    private java.util.List<config.other.CfgLootitem> ListRefLootid;
    private java.util.List<config.other.CfgLootitem> ListRefAnotherWay;

    private CfgLoot() {
    }

    public static CfgLoot _create(configgen.genjava.ConfigInput input) {
        CfgLoot self = new CfgLoot();
        self.lootid = input.readInt();
        self.ename = input.readStringInPool();
        self.name = config.Text._create(input);
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.chanceList = java.util.Collections.emptyList();
            } else {
                self.chanceList = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.chanceList.add(input.readInt());
                }
                self.chanceList = java.util.Collections.unmodifiableList(self.chanceList);
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

    public java.util.List<config.other.CfgLootitem> listRefLootid() {
        return ListRefLootid;
    }

    public java.util.List<config.other.CfgLootitem> listRefAnotherWay() {
        return ListRefAnotherWay;
    }

    @Override
    public String toString() {
        return "(" + lootid + "," + ename + "," + name + "," + chanceList + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.ListRefLootid = new java.util.ArrayList<>();
        for (config.other.CfgLootitem v : mgr._get_other_lootitem_All().values()) {
            if (v.getLootid() == this.lootid)
                this.ListRefLootid.add(v);
        }
        this.ListRefLootid = java.util.Collections.unmodifiableList(this.ListRefLootid);
        this.ListRefAnotherWay = new java.util.ArrayList<>();
        for (config.other.CfgLootitem v : mgr._get_other_lootitem_All().values()) {
            if (v.getLootid() == this.lootid)
                this.ListRefAnotherWay.add(v);
        }
        this.ListRefAnotherWay = java.util.Collections.unmodifiableList(this.ListRefAnotherWay);

    }

    public static CfgLoot get(int lootid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherLoot(lootid);
    }

    public static java.util.Collection<CfgLoot> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherLoot();
    }


}
