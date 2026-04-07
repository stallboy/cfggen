namespace Config.Equip;

public partial class DataJewelryrandom
{
    public required DataLevelRank LvlRank { get; init; } /* 等级 */
    public required DataRange AttackRange { get; init; } /* 最小攻击力 */
    public required List<DataRange> OtherRange { get; init; } /* 最小防御力 */
    public required List<Equip.DataTestPackBean> TestPack { get; init; } /* 测试pack */
    
    private static OrderedDictionary<DataLevelRank, DataJewelryrandom> _all = [];

    public static DataJewelryrandom? Get(DataLevelRank lvlRank)
    {
        return _all.GetValueOrDefault(lvlRank);
    }

    public static IReadOnlyList<DataJewelryrandom> All()
    {
        return _all.Values;
    }
}
