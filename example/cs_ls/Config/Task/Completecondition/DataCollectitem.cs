namespace Config.Task.Completecondition;

public partial class DataCollectItem : Task.DataCompletecondition
{
    public override Task.DataCompleteconditiontype type() {
        return Task.DataCompleteconditiontype.CollectItem;
    }

    public required int Itemid { get; init; }
    public required int Count { get; init; }

    public override int GetHashCode()
    {
        return Itemid.GetHashCode() + Count.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataCollectItem;
        return o != null && Itemid.Equals(o.Itemid) && Count.Equals(o.Count);
    }

    public override string ToString()
    {
        return "(" + Itemid + "," + Count + ")";
    }

    internal new static DataCollectItem _create(Stream os)
    {
        var itemid = os.ReadInt32();
        var count = os.ReadInt32();
        return new DataCollectItem {
            Itemid = itemid,
            Count = count,
        };
    }

}
