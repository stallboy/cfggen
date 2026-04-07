namespace Config.Other;

public partial class DataLootitem
{
    public required int Lootid { get; init; } /* 掉落id */
    public required int Itemid { get; init; } /* 掉落物品 */
    public required int Chance { get; init; } /* 掉落概率 */
    public required int Countmin { get; init; } /* 数量下限 */
    public required int Countmax { get; init; } /* 数量上限 */
    
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

    private static OrderedDictionary<LootidItemidKey, DataLootitem> _all = [];

    public static DataLootitem? Get(int lootid, int itemid)
    {
        return _all.GetValueOrDefault(new LootidItemidKey(lootid, itemid));
    }

    public static IReadOnlyList<DataLootitem> All()
    {
        return _all.Values;
    }
}
