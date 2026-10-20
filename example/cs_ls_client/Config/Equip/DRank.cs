using System.Collections.Generic;
namespace Config.Equip
{

public enum DRank
{
    White,
    Green,
    Blue,
    Purple,
    Yellow,
    Red,
}

public partial class DRankInfo
{
    public int RankID { get; init; } /* 稀有度 */
    public string RankName { get; init; } = null!; /* 程序用名字 */
    public string RankShowName { get; init; } = null!; /* 显示名称 */
    public DRank EEnum { get; init; }

    public static DRankInfo? Get(int rankID)
        => Get(ConfigMgr.GetMgr(), rankID);

    public static DRankInfo? Get(ConfigMgr config, int rankID)
    {
        var index = config._equip_drank_All;
        var key = rankID;
        return key >= 0 && key < index.Length ? index[key] : null;
    }

    public static IReadOnlyList<DRankInfo> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DRankInfo> All(ConfigMgr config)
        => config._equip_drank_Rows;
}

public static class DRankExtensions
{
    public static DRankInfo Info(this DRank e)
        => e.Info(ConfigMgr.GetMgr());

    public static DRankInfo Info(this DRank e, ConfigMgr config)
        => config._equip_drank_Symbols[(int)e]!;
}
}
