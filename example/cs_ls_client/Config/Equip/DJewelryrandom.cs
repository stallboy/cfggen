using System.Collections.Generic;
namespace Config.Equip
{

public partial class DJewelryrandom
{
    public DLevelRank LvlRank { get; init; } = null!; /* 等级 */
    public DRange AttackRange { get; init; } = null!; /* 最小攻击力 */
    public IReadOnlyList<DRange> OtherRange { get; init; } = null!; /* 最小防御力 */
    public IReadOnlyList<Equip.DTestPackBean> TestPack { get; init; } = null!; /* 测试pack */

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
}
