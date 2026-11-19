package config.task.completecondition;

import org.jspecify.annotations.Nullable;

public final class CfgCollectItem implements config.task.completecondition.CfgCompletecondition {
    private final int itemid;
    private final int count;

    public CfgCollectItem(int itemid, int count) {
        this.itemid = itemid;
        this.count = count;
    }

    private CfgCollectItem(configgen.genjava.ConfigInput input) {
        this.itemid = input.readInt();
        this.count = input.readInt();
    }

    @Override
    public config.task.CfgComplete_condition_type type() {
        return config.task.CfgComplete_condition_type.COLLECTITEM;
    }

    public int getItemid() {
        return itemid;
    }

    public int getCount() {
        return count;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(itemid, count);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof CfgCollectItem o)) {
            return false;
        }
        return itemid == o.itemid && count == o.count;
    }

    @Override
    public String toString() {
        return "CfgCollectItem(" + itemid + "," + count + ")";
    }

    public static CfgCollectItem _create(configgen.genjava.ConfigInput input) {
        return new CfgCollectItem(input);
    }

}
