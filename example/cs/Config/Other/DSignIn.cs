namespace Config.Other;

public partial class DSignin
{
    public required int Id { get; init; } /* 礼包ID */
    public required IReadOnlyDictionary<int, int> Item2countMap { get; init; } /* 普通奖励 */
    public required IReadOnlyDictionary<int, int> Vipitem2vipcountMap { get; init; } /* vip奖励 */
    public required int Viplevel { get; init; } /* 领取vip奖励的最低等级 */
    public required string IconFile { get; init; } /* 礼包图标 */
    public IReadOnlyDictionary<int, Other.DLoot> RefVipitem2vipcountMap { get; private set; } = null!;

    public static DSignin? Get(int id)
        => Get(ConfigMgr.GetMgr(), id);

    public static DSignin? Get(ConfigMgr config, int id)
    {
        var index = config._other_dsignin_All;
        return index.TryGetValue(id, out var result) ? result : null;
    }

    public static IReadOnlyList<DSignin> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DSignin> All(ConfigMgr config)
        => config._other_dsignin_Rows;
}
