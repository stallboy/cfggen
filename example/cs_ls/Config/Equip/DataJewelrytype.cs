using System.Collections.Generic;
namespace Config.Equip;

public partial class DataJewelrytype
{
    public static DataJewelrytype Jade { get; private set; } = null!;
    public static DataJewelrytype Bracelet { get; private set; } = null!;
    public static DataJewelrytype Magic { get; private set; } = null!;
    public static DataJewelrytype Bottle { get; private set; } = null!;
    public required string TypeName { get; init; } /* 程序用名字 */

    public override int GetHashCode()
    {
        return TypeName.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataJewelrytype;
        return o != null && TypeName.Equals(o.TypeName);
    }

    public override string ToString()
    {
        return "(" + TypeName + ")";
    }

    
    private static OrderedDictionary<string, DataJewelrytype> _all = [];

    public static DataJewelrytype? Get(string typeName)
    {
        return _all.GetValueOrDefault(typeName);
    }

    public static IReadOnlyList<DataJewelrytype> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(self.TypeName, self);
            if (self.TypeName.Trim().Length == 0)
                continue;
            switch(self.TypeName.Trim())
            {
                case "Jade":
                    if (Jade != null)
                        errors.EnumDup("equip.jewelrytype", self.ToString());
                    Jade = self;
                    break;
                case "Bracelet":
                    if (Bracelet != null)
                        errors.EnumDup("equip.jewelrytype", self.ToString());
                    Bracelet = self;
                    break;
                case "Magic":
                    if (Magic != null)
                        errors.EnumDup("equip.jewelrytype", self.ToString());
                    Magic = self;
                    break;
                case "Bottle":
                    if (Bottle != null)
                        errors.EnumDup("equip.jewelrytype", self.ToString());
                    Bottle = self;
                    break;
                default:
                    errors.EnumDataAdd("equip.jewelrytype", self.ToString());
                    break;
            }
        }

        if (Jade == null)
            errors.EnumNull("equip.jewelrytype", "Jade");
        if (Bracelet == null)
            errors.EnumNull("equip.jewelrytype", "Bracelet");
        if (Magic == null)
            errors.EnumNull("equip.jewelrytype", "Magic");
        if (Bottle == null)
            errors.EnumNull("equip.jewelrytype", "Bottle");
    }

    internal static DataJewelrytype _create(Stream os)
    {
        var typeName = os.ReadStringInPool();
        return new DataJewelrytype {
            TypeName = typeName,
        };
    }

}
