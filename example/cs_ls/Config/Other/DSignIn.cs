namespace Config.Other;

public partial class DSignIn
{
    public required int Id { get; init; } /* 礼包ID */
    public required IReadOnlyDictionary<int, int> Item2countMap { get; init; } /* 普通奖励 */
    public required IReadOnlyDictionary<int, int> Vipitem2vipcountMap { get; init; } /* vip奖励 */
    public required int Viplevel { get; init; } /* 领取vip奖励的最低等级 */
    public required string IconFile { get; init; } /* 礼包图标 */
    public IReadOnlyDictionary<int, Other.DLoot> RefVipitem2vipcountMap { get; private set; } = null!;

    public static DSignIn? Get(int id)
        => Get(ConfigMgr.GetMgr(), id);

    public static DSignIn? Get(ConfigMgr config, int id)
    {
        var index = config._other_dsign_uin_All;
        return index.TryGetValue(id, out var result) ? result : null;
    }

    public static IReadOnlyList<DSignIn> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DSignIn> All(ConfigMgr config)
        => config._other_dsign_uin_Rows;
}
