namespace Config.Equip;

public partial class DataRank
{
    public static DataRank White { get; private set; } = null!;
    public static DataRank Green { get; private set; } = null!;
    public static DataRank Blue { get; private set; } = null!;
    public static DataRank Purple { get; private set; } = null!;
    public static DataRank Yellow { get; private set; } = null!;

    public required int RankID { get; init; } /* 稀有度 */
    public required string RankName { get; init; } /* 程序用名字 */
    public required string RankShowName { get; init; } /* 显示名称 */
    
    private static OrderedDictionary<int, DataRank> _all = [];

    public static DataRank? Get(int rankID)
    {
        return _all.GetValueOrDefault(rankID);
    }

    public static IReadOnlyList<DataRank> All()
    {
        return _all.Values;
    }
}
