package config.task.completecondition;

public final class CfgCollectItem implements config.task.completecondition.CfgCompletecondition {
    @Override
    public config.task.CfgCompleteconditiontype type() {
        return config.task.CfgCompleteconditiontype.COLLECTITEM;
    }

    private int itemid;
    private int count;

    private CfgCollectItem() {
    }

    public CfgCollectItem(int itemid, int count) {
        this.itemid = itemid;
        this.count = count;
    }

    public static CfgCollectItem _create(configgen.genjava.ConfigInput input) {
        CfgCollectItem self = new CfgCollectItem();
        self.itemid = input.readInt();
        self.count = input.readInt();
        return self;
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
    public boolean equals(Object other) {
        if (!(other instanceof CfgCollectItem))
            return false;
        CfgCollectItem o = (CfgCollectItem) other;
        return itemid == o.itemid && count == o.count;
    }

    @Override
    public String toString() {
        return "CfgCollectItem(" + itemid + "," + count + ")";
    }

}
