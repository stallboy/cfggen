using System.Collections.Generic;
namespace Config.Equip;

public partial class DataJewelrysuit
{
    public static DataJewelrysuit SpecialSuit { get; private set; } = null!;
    public required int SuitID { get; init; } /* 饰品套装ID */
    public required string Ename { get; init; }
    public required string Name { get; init; } /* 策划用名字 */
    public required int Ability1 { get; init; } /* 套装属性类型1（装备套装中的两件时增加的属性） */
    public required int Ability1Value { get; init; } /* 套装属性1 */
    public required int Ability2 { get; init; } /* 套装属性类型2（装备套装中的三件时增加的属性） */
    public required int Ability2Value { get; init; } /* 套装属性2 */
    public required int Ability3 { get; init; } /* 套装属性类型3（装备套装中的四件时增加的属性） */
    public required int Ability3Value { get; init; } /* 套装属性3 */
    public required List<int> SuitList { get; init; } /* 部件1 */

    public override int GetHashCode()
    {
        return SuitID.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataJewelrysuit;
        return o != null && SuitID.Equals(o.SuitID);
    }

    public override string ToString()
    {
        return "(" + SuitID + "," + Ename + "," + Name + "," + Ability1 + "," + Ability1Value + "," + Ability2 + "," + Ability2Value + "," + Ability3 + "," + Ability3Value + "," + StringUtil.ToString(SuitList) + ")";
    }

    
    private static OrderedDictionary<int, DataJewelrysuit> _all = [];

    public static DataJewelrysuit? Get(int suitID)
    {
        return _all.GetValueOrDefault(suitID);
    }

    public static IReadOnlyList<DataJewelrysuit> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(self.SuitID, self);
            if (self.Ename.Trim().Length == 0)
                continue;
            switch(self.Ename.Trim())
            {
                case "SpecialSuit":
                    if (SpecialSuit != null)
                        errors.EnumDup("equip.jewelrysuit", self.ToString());
                    SpecialSuit = self;
                    break;
                default:
                    errors.EnumDataAdd("equip.jewelrysuit", self.ToString());
                    break;
            }
        }

        if (SpecialSuit == null)
            errors.EnumNull("equip.jewelrysuit", "SpecialSuit");
    }

    internal static DataJewelrysuit _create(Stream os)
    {
        var suitID = os.ReadInt32();
        var ename = os.ReadStringInPool();
        var name = os.ReadTextInPool();
        var ability1 = os.ReadInt32();
        var ability1Value = os.ReadInt32();
        var ability2 = os.ReadInt32();
        var ability2Value = os.ReadInt32();
        var ability3 = os.ReadInt32();
        var ability3Value = os.ReadInt32();
        List<int> suitList = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            suitList.Add(os.ReadInt32());
        return new DataJewelrysuit {
            SuitID = suitID,
            Ename = ename,
            Name = name,
            Ability1 = ability1,
            Ability1Value = ability1Value,
            Ability2 = ability2,
            Ability2Value = ability2Value,
            Ability3 = ability3,
            Ability3Value = ability3Value,
            SuitList = suitList,
        };
    }

}
