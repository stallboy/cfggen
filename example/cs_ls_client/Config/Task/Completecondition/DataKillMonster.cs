namespace Config.Task.Completecondition;

public partial class DataKillMonster : Task.DataCompletecondition
{
    public override Task.DataCompleteconditiontype type() {
        return Task.DataCompleteconditiontype.KillMonster;
    }

    public required int Monsterid { get; init; }
    public required int Count { get; init; }
    public Other.DataMonster RefMonsterid { get; private set; } = null!;

    public override int GetHashCode()
    {
        return Monsterid.GetHashCode() + Count.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataKillMonster;
        return o != null && Monsterid.Equals(o.Monsterid) && Count.Equals(o.Count);
    }

    public override string ToString()
    {
        return "(" + Monsterid + "," + Count + ")";
    }

    internal new static DataKillMonster _create(Stream os)
    {
        var monsterid = os.ReadInt32();
        var count = os.ReadInt32();
        return new DataKillMonster {
            Monsterid = monsterid,
            Count = count,
        };
    }

    internal override void _resolve(LoadErrors errors)
    {
        RefMonsterid = Other.DataMonster.Get(Monsterid)!;
        if (RefMonsterid == null) errors.RefNull("KillMonster", ToString(), "monsterid");
    }
}
