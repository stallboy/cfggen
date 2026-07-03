using System;
using System.Collections.Generic;
namespace Config.Other
{

public partial class DLootitem
{
    public int Lootid { get; init; } /* 掉落id */
    public int Itemid { get; init; } /* 掉落物品 */
    public int Chance { get; init; } /* 掉落概率 */
    public int Countmin { get; init; } /* 数量下限 */
    public int Countmax { get; init; } /* 数量上限 */
    private static IReadOnlyList<DLootitem> _allList = null!;
    
    class LootidItemidKey
    {
        readonly int Lootid;
        readonly int Itemid;
        public LootidItemidKey(int lootid, int itemid)
        {
            Lootid = lootid;
            Itemid = itemid;
        }

        public override int GetHashCode()
        {
            return Lootid.GetHashCode() + Itemid.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as LootidItemidKey;
            return o != null && Lootid.Equals(o.Lootid) && Itemid.Equals(o.Itemid);
        }
    }

    private static Dictionary<LootidItemidKey, DLootitem> _all = null!;

    public static DLootitem? Get(int lootid, int itemid)
    {
        return _all.GetValueOrDefault(new LootidItemidKey(lootid, itemid));
    }

    public static IReadOnlyList<DLootitem> All()
    {
        return _allList;
    }
}
}
