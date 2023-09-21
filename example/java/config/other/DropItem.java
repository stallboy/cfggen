package config.other;

public class DropItem {
    private int chance;
    private java.util.List<Integer> itemids;
    private int countmin;
    private int countmax;

    private DropItem() {
    }

    public DropItem(int chance, java.util.List<Integer> itemids, int countmin, int countmax) {
        this.chance = chance;
        this.itemids = itemids;
        this.countmin = countmin;
        this.countmax = countmax;
    }

    public static DropItem _create(configgen.genjava.ConfigInput input) {
        DropItem self = new DropItem();
        self.chance = input.readInt();
        self.itemids = new java.util.ArrayList<>();
        for (int c = input.readInt(); c > 0; c--) {
            self.itemids.add(input.readInt());
        }
        self.countmin = input.readInt();
        self.countmax = input.readInt();
        return self;
    }

    /**
     * 掉落概率
     */
    public int getChance() {
        return chance;
    }

    /**
     * 掉落物品
     */
    public java.util.List<Integer> getItemids() {
        return itemids;
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
    public int hashCode() {
        return chance + itemids.hashCode() + countmin + countmax;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DropItem))
            return false;
        DropItem o = (DropItem) other;
        return chance == o.chance && itemids.equals(o.itemids) && countmin == o.countmin && countmax == o.countmax;
    }

    @Override
    public String toString() {
        return "(" + chance + "," + itemids + "," + countmin + "," + countmax + ")";
    }

}
