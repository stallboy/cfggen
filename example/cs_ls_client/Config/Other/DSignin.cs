namespace Config.Other;

public partial class DSignin
{
    public required int Id { get; init; } /* 礼包ID */
    public required OrderedDictionary<int, int> Item2countMap { get; init; } /* 普通奖励 */
    public required OrderedDictionary<int, int> Vipitem2vipcountMap { get; init; } /* vip奖励 */
    public required int Viplevel { get; init; } /* 领取vip奖励的最低等级 */
    public required string IconFile { get; init; } /* 礼包图标 */
    public OrderedDictionary<int, Other.DLoot> RefVipitem2vipcountMap { get; private set; } = null!;
    
    private static System.Collections.Frozen.FrozenDictionary<int, DSignin> _all = null!;

    public static DSignin? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DSignin> All()
    {
        return _all.Values;
    }
}
