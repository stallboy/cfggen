using System;
using System.Collections.Generic;
namespace Config.Other
{

public partial class DMonster
{
    public int Id { get; init; }
    public List<DPosition> PosList { get; init; } = null!;
    public int LootId { get; init; } /* loot */
    public int LootItemId { get; init; } /* item */
    public OrderedDictionary<string, int> EnumMap1 { get; init; } = null!;
    public OrderedDictionary<int, string> EnumMap2 { get; init; } = null!;
    public Other.DLootitem RefLoot { get; private set; } = null!;
    public Other.DLoot RefAllLoot { get; private set; } = null!;
    public OrderedDictionary<int, Other.DArgCaptureMode> RefEnumMap2 { get; private set; } = null!;
    private static IReadOnlyList<DMonster> _allList = null!;
    
    private static Dictionary<int, DMonster> _all = null!;

    public static DMonster? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DMonster> All()
    {
        return _allList;
    }
}
}
