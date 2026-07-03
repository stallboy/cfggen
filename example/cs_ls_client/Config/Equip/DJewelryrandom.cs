using System;
using System.Collections.Generic;
namespace Config.Equip
{

public partial class DJewelryrandom
{
    public DLevelRank LvlRank { get; init; } = null!; /* 等级 */
    public DRange AttackRange { get; init; } = null!; /* 最小攻击力 */
    public List<DRange> OtherRange { get; init; } = null!; /* 最小防御力 */
    public List<Equip.DTestPackBean> TestPack { get; init; } = null!; /* 测试pack */
    private static IReadOnlyList<DJewelryrandom> _allList = null!;
    
    private static Dictionary<DLevelRank, DJewelryrandom> _all = null!;

    public static DJewelryrandom? Get(DLevelRank lvlRank)
    {
        return _all.GetValueOrDefault(lvlRank);
    }

    public static IReadOnlyList<DJewelryrandom> All()
    {
        return _allList;
    }
}
}
