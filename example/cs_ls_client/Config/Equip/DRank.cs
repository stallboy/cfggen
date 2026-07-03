using System;
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
    
    private static DRankInfo[] _all = null!;

    public static DRankInfo? Get(int rankID)
    {
        var key = rankID;
        return key >= 0 && key < _all.Length ? _all[key] : null;
    }

    public static IReadOnlyList<DRankInfo> All()
    {
        return _all;
    }
}

public static class DRankExtensions
{
    internal static readonly DRankInfo[] _infos = new DRankInfo[6];

    public static DRankInfo Info(this DRank e)
    {
        return _infos[(int)e];
    }
}
}
