package config.equip;

import org.jspecify.annotations.Nullable;

// 来自：equip/jewelry_suit.csv
public class CfgJewelry_suit {
    private final int suitID;
    private final String ename;
    private final config.Text name;
    private final int ability1;
    private final int ability1Value;
    private final int ability2;
    private final int ability2Value;
    private final int ability3;
    private final int ability3Value;
    private final java.util.List<Integer> suitList;

    private CfgJewelry_suit(configgen.genjava.ConfigInput input) {
        this.suitID = input.readInt();
        this.ename = input.readStringInPool();
        this.name = config.Text._create(input);
        this.ability1 = input.readInt();
        this.ability1Value = input.readInt();
        this.ability2 = input.readInt();
        this.ability2Value = input.readInt();
        this.ability3 = input.readInt();
        this.ability3Value = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.suitList = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<Integer>(c);
                for (; c > 0; c--) {
                    items.add(input.readInt());
                }
                this.suitList = java.util.Collections.unmodifiableList(items);
            }
        }
    }

    /**
     * 饰品套装ID
     */
    public int getSuitID() {
        return suitID;
    }

    public String getEname() {
        return ename;
    }

    /**
     * 策划用名字
     */
    public config.Text getName() {
        return name;
    }

    /**
     * 套装属性类型1（装备套装中的两件时增加的属性）
     */
    public int getAbility1() {
        return ability1;
    }

    /**
     * 套装属性1
     */
    public int getAbility1Value() {
        return ability1Value;
    }

    /**
     * 套装属性类型2（装备套装中的三件时增加的属性）
     */
    public int getAbility2() {
        return ability2;
    }

    /**
     * 套装属性2
     */
    public int getAbility2Value() {
        return ability2Value;
    }

    /**
     * 套装属性类型3（装备套装中的四件时增加的属性）
     */
    public int getAbility3() {
        return ability3;
    }

    /**
     * 套装属性3
     */
    public int getAbility3Value() {
        return ability3Value;
    }

    /**
     * 部件1
     */
    public java.util.List<Integer> getSuitList() {
        return suitList;
    }

    @Override
    public String toString() {
        return "(" + suitID + "," + ename + "," + name + "," + ability1 + "," + ability1Value + "," + ability2 + "," + ability2Value + "," + ability3 + "," + ability3Value + "," + suitList + ")";
    }

    public static CfgJewelry_suit _create(configgen.genjava.ConfigInput input) {
        return new CfgJewelry_suit(input);
    }

    public static @Nullable CfgJewelry_suit get(int suitID) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getEquipJewelry_suit(suitID);
    }

    public static java.util.Collection<CfgJewelry_suit> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allEquipJewelry_suit();
    }


}
