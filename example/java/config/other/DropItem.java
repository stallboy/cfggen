package config.other;

import org.jspecify.annotations.Nullable;

public class DropItem {
    private final int chance;
    private final java.util.List<Integer> itemids;
    private final int countmin;
    private final int countmax;

    public DropItem(int chance, java.util.List<Integer> itemids, int countmin, int countmax) {
        this.chance = chance;
        this.itemids = itemids;
        this.countmin = countmin;
        this.countmax = countmax;
    }

    private DropItem(configgen.genjava.ConfigInput input) {
        this.chance = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.itemids = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<Integer>(c);
                for (; c > 0; c--) {
                    items.add(input.readInt());
                }
                this.itemids = java.util.Collections.unmodifiableList(items);
            }
        }
        this.countmin = input.readInt();
        this.countmax = input.readInt();
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
        return java.util.Objects.hash(chance, itemids, countmin, countmax);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof DropItem o)) {
            return false;
        }
        return chance == o.chance && itemids.equals(o.itemids) && countmin == o.countmin && countmax == o.countmax;
    }

    @Override
    public String toString() {
        return "(" + chance + "," + itemids + "," + countmin + "," + countmax + ")";
    }

    public static DropItem _create(configgen.genjava.ConfigInput input) {
        return new DropItem(input);
    }

}
