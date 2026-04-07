namespace Config.Equip;

public partial class DataJewelry
{
    public required int ID { get; init; } /* 首饰ID */
    public required string Name { get; init; } /* 首饰名称 */
    public required string IconFile { get; init; } /* 图标ID */
    public required DataLevelRank LvlRank { get; init; } /* 首饰等级 */
    public required string JType { get; init; } /* 首饰类型 */
    public required int SuitID { get; init; } /* 套装ID（为0是没有不属于套装，首饰品级为4的首饰该参数为套装id，其余情况为0,引用JewelrySuit.csv） */
    public required int KeyAbility { get; init; } /* 关键属性类型 */
    public required int KeyAbilityValue { get; init; } /* 关键属性数值 */
    public required int SalePrice { get; init; } /* 售卖价格 */
    public required string Description { get; init; } /* 描述,根据Lvl和Rank来随机3个属性，第一个属性由Lvl,Rank行随机，剩下2个由Lvl和小于Rank的行里随机。Rank最小的时候都从Lvl，Rank里随机。 */
    public Equip.DataJewelryrandom RefLvlRank { get; private set; } = null!;
    public Equip.DataJewelrytype RefJType { get; private set; }
    public Equip.DataJewelrysuit? NullableRefSuitID { get; private set; }
    public Equip.DataAbility RefKeyAbility { get; private set; }
    
    private static OrderedDictionary<int, DataJewelry> _all = [];

    public static DataJewelry? Get(int iD)
    {
        return _all.GetValueOrDefault(iD);
    }

    public static IReadOnlyList<DataJewelry> All()
    {
        return _all.Values;
    }
}
