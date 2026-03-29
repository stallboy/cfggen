using System.Collections.Generic;
namespace Config.Other;

public partial class DataLoot
{
    public required int Lootid { get; init; } /* 序号 */
    public required string Ename { get; init; }
    public required string Name { get; init; } /* 名字 */
    public required List<int> ChanceList { get; init; } /* 掉落0件物品的概率 */
    public List<Other.DataLootitem> ListRefLootid { get; private set; } = null!;
    public List<Other.DataLootitem> ListRefAnotherWay { get; private set; } = null!;

    public override int GetHashCode()
    {
        return Lootid.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataLoot;
        return o != null && Lootid.Equals(o.Lootid);
    }

    public override string ToString()
    {
        return "(" + Lootid + "," + Ename + "," + Name + "," + StringUtil.ToString(ChanceList) + ")";
    }

    
    private static OrderedDictionary<int, DataLoot> _all = [];

    public static DataLoot? Get(int lootid)
    {
        return _all.GetValueOrDefault(lootid);
    }

    public static IReadOnlyList<DataLoot> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(self.Lootid, self);
        }

    }

    internal static void Resolve(LoadErrors errors)
    {
        foreach (var v in All())
            v._resolve(errors);
    }
    internal static DataLoot _create(Stream os)
    {
        var lootid = os.ReadInt32();
        var ename = os.ReadStringInPool();
        var name = os.ReadTextInPool();
        List<int> chanceList = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            chanceList.Add(os.ReadInt32());
        return new DataLoot {
            Lootid = lootid,
            Ename = ename,
            Name = name,
            ChanceList = chanceList,
        };
    }

    internal void _resolve(LoadErrors errors)
    {
        ListRefLootid = [];
        foreach (var v in Other.DataLootitem.All())
        {
            if (v.Lootid.Equals(Lootid))
                ListRefLootid.Add(v);
        }
        ListRefAnotherWay = [];
        foreach (var v in Other.DataLootitem.All())
        {
            if (v.Lootid.Equals(Lootid))
                ListRefAnotherWay.Add(v);
        }
    }
}
