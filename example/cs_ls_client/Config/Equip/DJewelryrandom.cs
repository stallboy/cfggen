namespace Config.Equip;

public partial class DJewelryrandom
{
    public required DLevelRank LvlRank { get; init; } /* 等级 */
    public required DRange AttackRange { get; init; } /* 最小攻击力 */
    public required List<DRange> OtherRange { get; init; } /* 最小防御力 */
    public required List<Equip.DTestPackBean> TestPack { get; init; } /* 测试pack */
    
    private static System.Collections.Frozen.FrozenDictionary<DLevelRank, DJewelryrandom> _all = null!;

    public static DJewelryrandom? Get(DLevelRank lvlRank)
    {
        return _all.GetValueOrDefault(lvlRank);
    }

    public static IReadOnlyList<DJewelryrandom> All()
    {
        return _all.Values;
    }
}
