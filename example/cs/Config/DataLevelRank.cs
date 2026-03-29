namespace Config;

public partial class DataLevelRank
{
    public required int Level { get; init; } /* 等级 */
    public required int Rank { get; init; } /* 品质 */
    public Equip.DataRank RefRank { get; private set; } = null!;

    public override int GetHashCode()
    {
        return Level.GetHashCode() + Rank.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataLevelRank;
        return o != null && Level.Equals(o.Level) && Rank.Equals(o.Rank);
    }

    public override string ToString()
    {
        return "(" + Level + "," + Rank + ")";
    }

    internal static DataLevelRank _create(Stream os)
    {
        var level = os.ReadInt32();
        var rank = os.ReadInt32();
        return new DataLevelRank {
            Level = level,
            Rank = rank,
        };
    }

    internal void _resolve(LoadErrors errors)
    {
        RefRank = Equip.DataRank.Get(Rank)!;
        if (RefRank == null) errors.RefNull("LevelRank", ToString(), "Rank");
    }
}
