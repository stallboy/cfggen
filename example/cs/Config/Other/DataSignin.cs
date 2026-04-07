namespace Config.Other;

public partial class DataSignin
{
    public required int Id { get; init; } /* 礼包ID */
    public required OrderedDictionary<int, int> Item2countMap { get; init; } /* 普通奖励 */
    public required OrderedDictionary<int, int> Vipitem2vipcountMap { get; init; } /* vip奖励 */
    public required int Viplevel { get; init; } /* 领取vip奖励的最低等级 */
    public required string IconFile { get; init; } /* 礼包图标 */
    public OrderedDictionary<int, Other.DataLoot> RefVipitem2vipcountMap { get; private set; } = null!;
    
    private static OrderedDictionary<int, DataSignin> _all = [];

    public static DataSignin? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DataSignin> All()
    {
        return _all.Values;
    }
}
