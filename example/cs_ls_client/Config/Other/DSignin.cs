using System;
using System.Collections.Generic;
namespace Config.Other
{

public partial class DSignin
{
    public int Id { get; init; } /* 礼包ID */
    public OrderedDictionary<int, int> Item2countMap { get; init; } = null!; /* 普通奖励 */
    public OrderedDictionary<int, int> Vipitem2vipcountMap { get; init; } = null!; /* vip奖励 */
    public int Viplevel { get; init; } /* 领取vip奖励的最低等级 */
    public string IconFile { get; init; } = null!; /* 礼包图标 */
    public OrderedDictionary<int, Other.DLoot> RefVipitem2vipcountMap { get; private set; } = null!;
    private static IReadOnlyList<DSignin> _allList = null!;
    
    private static Dictionary<int, DSignin> _all = null!;

    public static DSignin? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DSignin> All()
    {
        return _allList;
    }
}
}
