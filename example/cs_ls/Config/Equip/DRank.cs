namespace Config.Equip;

public enum DRank
{
    White,
    Green,
    Blue,
    Purple,
    Yellow,
}

public partial class DRankInfo
{
    public required int RankID { get; init; } /* 稀有度 */
    public required string RankName { get; init; } /* 程序用名字 */
    public required string RankShowName { get; init; } /* 显示名称 */
    public required DRank EEnum { get; init; }
    
    private static System.Collections.Frozen.FrozenDictionary<int, DRankInfo> _all = null!;

    public static DRankInfo? Get(int rankID)
    {
        return _all.GetValueOrDefault(rankID);
    }

    public static IReadOnlyList<DRankInfo> All()
    {
        return _all.Values;
    }
}

public static class DRankExtensions
{
    internal static readonly DRankInfo[] _infos = new DRankInfo[5];

    public static DRankInfo Info(this DRank e)
    {
        return _infos[(int)e];
    }
}
