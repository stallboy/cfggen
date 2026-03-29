using System.Collections.Generic;
namespace Config.Other;

public partial class DataMonster
{
    public required int Id { get; init; }
    public required List<DataPosition> PosList { get; init; }
    public required int LootId { get; init; } /* loot */
    public required int LootItemId { get; init; } /* item */
    public required OrderedDictionary<string, int> EnumMap1 { get; init; }
    public required OrderedDictionary<int, string> EnumMap2 { get; init; }
    public Other.DataLootitem RefLoot { get; private set; } = null!;
    public Other.DataLoot RefAllLoot { get; private set; } = null!;
    public OrderedDictionary<int, Other.DataArgCaptureMode> RefEnumMap2 { get; private set; } = null!;

    public override int GetHashCode()
    {
        return Id.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataMonster;
        return o != null && Id.Equals(o.Id);
    }

    public override string ToString()
    {
        return "(" + Id + "," + StringUtil.ToString(PosList) + "," + LootId + "," + LootItemId + "," + EnumMap1 + "," + EnumMap2 + ")";
    }

    
    private static OrderedDictionary<int, DataMonster> _all = [];

    public static DataMonster? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DataMonster> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(self.Id, self);
        }

    }

    internal static void Resolve(LoadErrors errors)
    {
        foreach (var v in All())
            v._resolve(errors);
    }
    internal static DataMonster _create(Stream os)
    {
        var id = os.ReadInt32();
        List<DataPosition> posList = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            posList.Add(DataPosition._create(os));
        var lootId = os.ReadInt32();
        var lootItemId = os.ReadInt32();
        OrderedDictionary<string, int> enumMap1 = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            enumMap1.Add(os.ReadStringInPool(), os.ReadInt32());
        }
        OrderedDictionary<int, string> enumMap2 = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            enumMap2.Add(os.ReadInt32(), os.ReadStringInPool());
        }
        return new DataMonster {
            Id = id,
            PosList = posList,
            LootId = lootId,
            LootItemId = lootItemId,
            EnumMap1 = enumMap1,
            EnumMap2 = enumMap2,
        };
    }

    internal void _resolve(LoadErrors errors)
    {
        RefLoot = Other.DataLootitem.Get(LootId, LootItemId)!;
        if (RefLoot == null) errors.RefNull("other.monster", ToString(), "Loot");
        RefAllLoot = Other.DataLoot.Get(LootId)!;
        if (RefAllLoot == null) errors.RefNull("other.monster", ToString(), "AllLoot");
        RefEnumMap2 = [];
        foreach(var kv in EnumMap2)
        {
            var k = kv.Key;
            var v = Other.DataArgCaptureMode.Get(kv.Value);
            if (v == null) errors.RefNull("other.monster", ToString(), "enumMap2");
            else RefEnumMap2.Add(k, v);
        }
    }
}
