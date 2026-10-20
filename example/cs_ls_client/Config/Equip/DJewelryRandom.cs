using System.Collections.Generic;
namespace Config.Equip
{

public partial class DJewelryRandom
{
    public DLevelRank LvlRank { get; init; } = null!; /* 等级 */
    public DRange AttackRange { get; init; } = null!; /* 最小攻击力 */
    public IReadOnlyList<DRange> OtherRange { get; init; } = null!; /* 最小防御力 */
    public IReadOnlyList<Equip.DTestPackBean> TestPack { get; init; } = null!; /* 测试pack */

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
}
