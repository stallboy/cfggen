package config.equip;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

// 来自：equip/jewelry.csv
@SuppressWarnings("NotNullFieldNotInitialized")
public class Jewelry {
    private final int iD;
    private final String name;
    private final String iconFile;
    private final config.LevelRank lvlRank;
    private final String jType;
    private final int suitID;
    private final int keyAbility;
    private final int keyAbilityValue;
    private final int salePrice;
    private final String description;
    private config.equip.JewelryRandom RefLvlRank;
    private config.equip.JewelryType RefJType;
    private config.equip.@Nullable JewelrySuit NullableRefSuitID;
    private config.equip.Ability RefKeyAbility;

    Jewelry(JewelryBuilder b) {
        this.iD = b.iD;
        this.name = b.name;
        this.iconFile = b.iconFile;
        this.lvlRank = b.lvlRank;
        this.jType = b.jType;
        this.suitID = b.suitID;
        this.keyAbility = b.keyAbility;
        this.keyAbilityValue = b.keyAbilityValue;
        this.salePrice = b.salePrice;
        this.description = b.description;
    }

    private Jewelry(configgen.genjava.ConfigInput input) {
        this.iD = input.readInt();
        this.name = input.readStringInPool();
        this.iconFile = input.readStringInPool();
        this.lvlRank = config.LevelRank._create(input);
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
    public config.LevelRank getLvlRank() {
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

    public config.equip.JewelryRandom refLvlRank() {
        return RefLvlRank;
    }

    public config.equip.JewelryType refJType() {
        return RefJType;
    }

    public config.equip.@Nullable JewelrySuit nullableRefSuitID() {
        return NullableRefSuitID;
    }

    public config.equip.Ability refKeyAbility() {
        return RefKeyAbility;
    }

    @Override
    public String toString() {
        return "(" + iD + "," + name + "," + iconFile + "," + lvlRank + "," + jType + "," + suitID + "," + keyAbility + "," + keyAbilityValue + "," + salePrice + "," + description + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.lvlRank._resolve(mgr, errors);
        var refLvlRank = mgr.getEquipJewelryRandom(this.lvlRank);
        if (refLvlRank == null) {
            errors.missingReference("equip.jewelry", "LvlRank", "equip.jewelry_random", this.lvlRank);
        } else {
            this.RefLvlRank = refLvlRank;
        }
        var refJType = config.equip.JewelryType.get(this.jType);
        if (refJType == null) {
            errors.missingReference("equip.jewelry", "JType", "equip.jewelry_type", this.jType);
        } else {
            this.RefJType = refJType;
        }
        this.NullableRefSuitID = mgr.getEquipJewelrySuit(this.suitID);
        var refKeyAbility = config.equip.Ability.get(this.keyAbility);
        if (refKeyAbility == null) {
            errors.missingReference("equip.jewelry", "KeyAbility", "equip.ability", this.keyAbility);
        } else {
            this.RefKeyAbility = refKeyAbility;
        }

    }

    public static Jewelry _create(configgen.genjava.ConfigInput input) {
        return new Jewelry(input);
    }

    public static @Nullable Jewelry get(int iD) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getEquipJewelry(iD);
    }

    public static java.util.Collection<Jewelry> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allEquipJewelry();
    }


}
