package config.task.completecondition;

public final class CollectItem implements config.task.completecondition.Completecondition {
    private final int itemid;
    private final int count;

    public CollectItem(int itemid, int count) {
        this.itemid = itemid;
        this.count = count;
    }

    private CollectItem(configgen.genjava.ConfigInput input) {
        this.itemid = input.readInt();
        this.count = input.readInt();
    }

    @Override
    public config.task.CompleteConditionType type() {
        return config.task.CompleteConditionType.COLLECT_ITEM;
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
        if (!(other instanceof CollectItem o)) {
            return false;
        }
        return itemid == o.itemid && count == o.count;
    }

    @Override
    public String toString() {
        return "CollectItem(" + itemid + "," + count + ")";
    }

    public static CollectItem _create(configgen.genjava.ConfigInput input) {
        return new CollectItem(input);
    }

}
