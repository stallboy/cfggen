namespace Config.Equip;

public partial class DJewelryrandom
{
    public required DLevelRank LvlRank { get; init; } /* 等级 */
    public required DRange AttackRange { get; init; } /* 最小攻击力 */
    public required IReadOnlyList<DRange> OtherRange { get; init; } /* 最小防御力 */
    public required IReadOnlyList<Equip.DTestPackBean> TestPack { get; init; } /* 测试pack */

    public static DJewelryrandom? Get(DLevelRank lvlRank)
        => Get(ConfigMgr.GetMgr(), lvlRank);

    public static DJewelryrandom? Get(ConfigMgr config, DLevelRank lvlRank)
    {
        var index = config._equip_djewelryrandom_All;
        return index.TryGetValue(lvlRank, out var result) ? result : null;
    }

    public static IReadOnlyList<DJewelryrandom> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DJewelryrandom> All(ConfigMgr config)
        => config._equip_djewelryrandom_Rows;
}
