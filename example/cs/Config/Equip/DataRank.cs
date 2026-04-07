namespace Config.Equip;

public enum DataRank
{
    White,
    Green,
    Blue,
    Purple,
    Yellow,
}

public partial class DataRankInfo
{
    public required int RankID { get; init; } /* 稀有度 */
    public required string RankName { get; init; } /* 程序用名字 */
    public required string RankShowName { get; init; } /* 显示名称 */
    public required DataRank eEnum { get; init; }
    
    private static OrderedDictionary<int, DataRankInfo> _all = [];

    public static DataRankInfo? Get(int rankID)
    {
        return _all.GetValueOrDefault(rankID);
    }

    public static IReadOnlyList<DataRankInfo> All()
    {
        return _all.Values;
    }
}

public static class DataRankExtensions
{
    internal static readonly DataRankInfo[] _infos = new DataRankInfo[5];

    public static DataRankInfo Info(this DataRank e)
    {
        return _infos[(int)e];
    }
}
