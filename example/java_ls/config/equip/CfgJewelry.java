package config.equip;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

// 来自：equip/jewelry.csv
public class CfgJewelry {
    private final int iD;
    private final String name;
    private final String iconFile;
    private final config.CfgLevelRank lvlRank;
    private final String jType;
    private final int suitID;
    private final int keyAbility;
    private final int keyAbilityValue;
    private final int salePrice;
    private final String description;
    private config.equip.@Nullable CfgJewelry_random RefLvlRank;
    private config.equip.@Nullable CfgJewelry_type RefJType;
    private config.equip.@Nullable CfgJewelry_suit NullableRefSuitID;
    private config.equip.@Nullable CfgAbility RefKeyAbility;

    private CfgJewelry(configgen.genjava.ConfigInput input) {
        this.iD = input.readInt();
        this.name = input.readStringInPool();
        this.iconFile = input.readStringInPool();
        this.lvlRank = config.CfgLevelRank._create(input);
        this.jType = input.readStringInPool();
        this.suitID = input.readInt();
        this.keyAbility = input.readInt();
        this.keyAbilityValue = input.readInt();
        this.salePrice = input.readInt();
        this.description = input.readStringInPool();
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

    public config.equip.@Nullable CfgJewelry_random refLvlRank() {
        return RefLvlRank;
    }

    public config.equip.@Nullable CfgJewelry_type refJType() {
        return RefJType;
    }

    public config.equip.@Nullable CfgJewelry_suit nullableRefSuitID() {
        return NullableRefSuitID;
    }

    public config.equip.@Nullable CfgAbility refKeyAbility() {
        return RefKeyAbility;
    }

    @Override
    public String toString() {
        return "(" + iD + "," + name + "," + iconFile + "," + lvlRank + "," + jType + "," + suitID + "," + keyAbility + "," + keyAbilityValue + "," + salePrice + "," + description + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.lvlRank._resolve(mgr, errors);
        this.RefLvlRank = mgr.getEquipJewelry_random(this.lvlRank);
        if (this.RefLvlRank == null) {
            errors.missingReference("equip.jewelry", "LvlRank", "equip.jewelry_random", this.lvlRank);
        }
        this.RefJType = config.equip.CfgJewelry_type.get(this.jType);
        if (this.RefJType == null) {
            errors.missingReference("equip.jewelry", "JType", "equip.jewelry_type", this.jType);
        }
        this.NullableRefSuitID = mgr.getEquipJewelry_suit(this.suitID);
        this.RefKeyAbility = config.equip.CfgAbility.get(this.keyAbility);
        if (this.RefKeyAbility == null) {
            errors.missingReference("equip.jewelry", "KeyAbility", "equip.ability", this.keyAbility);
        }

    }

    public static CfgJewelry _create(configgen.genjava.ConfigInput input) {
        return new CfgJewelry(input);
    }

    public static @Nullable CfgJewelry get(int iD) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getEquipJewelry(iD);
    }

    public static java.util.Collection<CfgJewelry> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allEquipJewelry();
    }


}
