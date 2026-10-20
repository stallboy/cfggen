package config.equip;

import configgen.genjava.LoadValueErrs;

// 来自：equip/jewelry.csv
public class CfgJewelry {
    private int iD;
    private String name;
    private String iconFile;
    private config.CfgLevelRank lvlRank;
    private String jType;
    private int suitID;
    private int keyAbility;
    private int keyAbilityValue;
    private int salePrice;
    private String description;
    private config.equip.CfgJewelryrandom RefLvlRank;
    private config.equip.CfgJewelrytype RefJType;
    private config.equip.CfgJewelrysuit NullableRefSuitID;
    private config.equip.CfgAbility RefKeyAbility;

    private CfgJewelry() {
    }

    public static CfgJewelry _create(configgen.genjava.ConfigInput input) {
        CfgJewelry self = new CfgJewelry();
        self.iD = input.readInt();
        self.name = input.readStringInPool();
        self.iconFile = input.readStringInPool();
        self.lvlRank = config.CfgLevelRank._create(input);
        self.jType = input.readStringInPool();
        self.suitID = input.readInt();
        self.keyAbility = input.readInt();
        self.keyAbilityValue = input.readInt();
        self.salePrice = input.readInt();
        self.description = input.readStringInPool();
        return self;
    }

    /**
     * 首饰ID
     */
    public int getID() {
        return iD;
    }

    /**
     * 首饰名称
     */
    public String getName() {
        return name;
    }

    /**
     * 图标ID
     */
    public String getIconFile() {
        return iconFile;
    }

    /**
     * 首饰等级
     */
    public config.CfgLevelRank getLvlRank() {
        return lvlRank;
    }

    /**
     * 首饰类型
     */
    public String getJType() {
        return jType;
    }

    /**
     * 套装ID（为0是没有不属于套装，首饰品级为4的首饰该参数为套装id，其余情况为0,引用JewelrySuit.csv）
     */
    public int getSuitID() {
        return suitID;
    }

    /**
     * 关键属性类型
     */
    public int getKeyAbility() {
        return keyAbility;
    }

    /**
     * 关键属性数值
     */
    public int getKeyAbilityValue() {
        return keyAbilityValue;
    }

    /**
     * 售卖价格
     */
    public int getSalePrice() {
        return salePrice;
    }

    /**
     * 描述,根据Lvl和Rank来随机3个属性，第一个属性由Lvl,Rank行随机，剩下2个由Lvl和小于Rank的行里随机。Rank最小的时候都从Lvl，Rank里随机。
     */
    public String getDescription() {
        return description;
    }

    public config.equip.CfgJewelryrandom refLvlRank() {
        return RefLvlRank;
    }

    public config.equip.CfgJewelrytype refJType() {
        return RefJType;
    }

    public config.equip.CfgJewelrysuit nullableRefSuitID() {
        return NullableRefSuitID;
    }

    public config.equip.CfgAbility refKeyAbility() {
        return RefKeyAbility;
    }

    @Override
    public String toString() {
        return "(" + iD + "," + name + "," + iconFile + "," + lvlRank + "," + jType + "," + suitID + "," + keyAbility + "," + keyAbilityValue + "," + salePrice + "," + description + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.lvlRank._resolve(mgr, errors);
        this.RefLvlRank = mgr.getEquipJewelryrandom(this.lvlRank);
        if (this.RefLvlRank == null) errors.missingReference("equip.jewelry", "LvlRank", "equip.jewelryrandom", this.lvlRank);
        this.RefJType = config.equip.CfgJewelrytype.get(this.jType);
        if (this.RefJType == null) errors.missingReference("equip.jewelry", "JType", "equip.jewelrytype", this.jType);
        this.NullableRefSuitID = mgr.getEquipJewelrysuit(this.suitID);
        this.RefKeyAbility = config.equip.CfgAbility.get(this.keyAbility);
        if (this.RefKeyAbility == null) errors.missingReference("equip.jewelry", "KeyAbility", "equip.ability", this.keyAbility);

    }

    public static CfgJewelry get(int iD) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getEquipJewelry(iD);
    }

    public static java.util.Collection<CfgJewelry> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allEquipJewelry();
    }


}
