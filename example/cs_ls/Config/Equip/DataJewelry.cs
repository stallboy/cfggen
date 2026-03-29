using System.Collections.Generic;
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
    public Equip.DataJewelrytype RefJType { get; private set; } = null!;
    public Equip.DataJewelrysuit? NullableRefSuitID { get; private set; }
    public Equip.DataAbility RefKeyAbility { get; private set; } = null!;

    public override int GetHashCode()
    {
        return ID.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataJewelry;
        return o != null && ID.Equals(o.ID);
    }

    public override string ToString()
    {
        return "(" + ID + "," + Name + "," + IconFile + "," + LvlRank + "," + JType + "," + SuitID + "," + KeyAbility + "," + KeyAbilityValue + "," + SalePrice + "," + Description + ")";
    }

    
    private static OrderedDictionary<int, DataJewelry> _all = [];

    public static DataJewelry? Get(int iD)
    {
        return _all.GetValueOrDefault(iD);
    }

    public static IReadOnlyList<DataJewelry> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(self.ID, self);
        }

    }

    internal static void Resolve(LoadErrors errors)
    {
        foreach (var v in All())
            v._resolve(errors);
    }
    internal static DataJewelry _create(Stream os)
    {
        var iD = os.ReadInt32();
        var name = os.ReadStringInPool();
        var iconFile = os.ReadStringInPool();
        var lvlRank = DataLevelRank._create(os);
        var jType = os.ReadStringInPool();
        var suitID = os.ReadInt32();
        var keyAbility = os.ReadInt32();
        var keyAbilityValue = os.ReadInt32();
        var salePrice = os.ReadInt32();
        var description = os.ReadStringInPool();
        return new DataJewelry {
            ID = iD,
            Name = name,
            IconFile = iconFile,
            LvlRank = lvlRank,
            JType = jType,
            SuitID = suitID,
            KeyAbility = keyAbility,
            KeyAbilityValue = keyAbilityValue,
            SalePrice = salePrice,
            Description = description,
        };
    }

    internal void _resolve(LoadErrors errors)
    {
        LvlRank._resolve(errors);
        RefLvlRank = Equip.DataJewelryrandom.Get(LvlRank)!;
        if (RefLvlRank == null) errors.RefNull("equip.jewelry", ToString(), "LvlRank");
        RefJType = Equip.DataJewelrytype.Get(JType)!;
        if (RefJType == null) errors.RefNull("equip.jewelry", ToString(), "JType");
        NullableRefSuitID = Equip.DataJewelrysuit.Get(SuitID);
        RefKeyAbility = Equip.DataAbility.Get(KeyAbility)!;
        if (RefKeyAbility == null) errors.RefNull("equip.jewelry", ToString(), "KeyAbility");
    }
}
