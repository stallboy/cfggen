namespace Config.Task.Completecondition;

public partial class DataTalkNpc : Task.DataCompletecondition
{
    public override Task.DataCompleteconditiontype type() {
        return Task.DataCompleteconditiontype.TalkNpc;
    }

    public required int Npcid { get; init; }

    public override int GetHashCode()
    {
        return Npcid.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataTalkNpc;
        return o != null && Npcid.Equals(o.Npcid);
    }

    public override string ToString()
    {
        return "(" + Npcid + ")";
    }

    internal new static DataTalkNpc _create(Stream os)
    {
        var npcid = os.ReadInt32();
        return new DataTalkNpc {
            Npcid = npcid,
        };
    }

}
