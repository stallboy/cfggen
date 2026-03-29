using System.Collections.Generic;
namespace Config.Other;

public partial class DataDropItem
{
    public required int Chance { get; init; } /* 掉落概率 */
    public required List<int> Itemids { get; init; } /* 掉落物品 */
    public required int Countmin { get; init; } /* 数量下限 */
    public required int Countmax { get; init; } /* 数量上限 */

    public override int GetHashCode()
    {
        return Chance.GetHashCode() + Itemids.GetHashCode() + Countmin.GetHashCode() + Countmax.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataDropItem;
        return o != null && Chance.Equals(o.Chance) && Itemids.Equals(o.Itemids) && Countmin.Equals(o.Countmin) && Countmax.Equals(o.Countmax);
    }

    public override string ToString()
    {
        return "(" + Chance + "," + StringUtil.ToString(Itemids) + "," + Countmin + "," + Countmax + ")";
    }

    internal static DataDropItem _create(Stream os)
    {
        var chance = os.ReadInt32();
        List<int> itemids = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            itemids.Add(os.ReadInt32());
        var countmin = os.ReadInt32();
        var countmax = os.ReadInt32();
        return new DataDropItem {
            Chance = chance,
            Itemids = itemids,
            Countmin = countmin,
            Countmax = countmax,
        };
    }

}
