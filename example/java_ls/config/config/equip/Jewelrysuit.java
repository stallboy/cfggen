package config.equip;

public class Jewelrysuit {
    private int suitID;
    private String ename;
    private String name;
    private int ability1;
    private int ability1Value;
    private int ability2;
    private int ability2Value;
    private int ability3;
    private int ability3Value;
    private java.util.List<Integer> suitList;

    private Jewelrysuit() {
    }

    public static Jewelrysuit _create(configgen.genjava.ConfigInput input) {
        Jewelrysuit self = new Jewelrysuit();
        self.suitID = input.readInt();
        self.ename = input.readStringInPool();
        self.name = input.readTextInPool();
        self.ability1 = input.readInt();
        self.ability1Value = input.readInt();
        self.ability2 = input.readInt();
        self.ability2Value = input.readInt();
        self.ability3 = input.readInt();
        self.ability3Value = input.readInt();
        {
            int c = input.readInt();
            if (c == 0) {
                self.suitList = java.util.Collections.emptyList();
            } else {
                self.suitList = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.suitList.add(input.readInt());
                }
            }
        }
        return self;
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
    public String getName() {
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

    public static Jewelrysuit get(int suitID) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getEquipJewelrysuit(suitID);
    }

    public static java.util.Collection<Jewelrysuit> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allEquipJewelrysuit();
    }

    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                Jewelrysuit self = Jewelrysuit._create(input);
                mgr.equip_jewelrysuit_All.put(self.suitID, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            // no resolve
        }

    }

}
