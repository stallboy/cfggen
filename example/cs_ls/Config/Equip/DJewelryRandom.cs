namespace Config.Equip;

public partial class DJewelryRandom
{
    public required DLevelRank LvlRank { get; init; } /* 等级 */
    public required DRange AttackRange { get; init; } /* 最小攻击力 */
    public required IReadOnlyList<DRange> OtherRange { get; init; } /* 最小防御力 */
    public required IReadOnlyList<Equip.DTestPackBean> TestPack { get; init; } /* 测试pack */

    public static DJewelryRandom? Get(DLevelRank lvlRank)
        => Get(ConfigMgr.GetMgr(), lvlRank);

    public static DJewelryRandom? Get(ConfigMgr config, DLevelRank lvlRank)
    {
        var index = config._equip_djewelry_urandom_All;
        return index.TryGetValue(lvlRank, out var result) ? result : null;
    }

    public static IReadOnlyList<DJewelryRandom> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DJewelryRandom> All(ConfigMgr config)
        => config._equip_djewelry_urandom_Rows;
}
