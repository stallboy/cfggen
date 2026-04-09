namespace Config.Other;

public partial class DMonster
{
    public required int Id { get; init; }
    public required List<DPosition> PosList { get; init; }
    public required int LootId { get; init; } /* loot */
    public required int LootItemId { get; init; } /* item */
    public required OrderedDictionary<string, int> EnumMap1 { get; init; }
    public required OrderedDictionary<int, string> EnumMap2 { get; init; }
    public Other.DLootitem RefLoot { get; private set; } = null!;
    public Other.DLoot RefAllLoot { get; private set; } = null!;
    public OrderedDictionary<int, Other.DArgCaptureMode> RefEnumMap2 { get; private set; } = null!;
    
    private static System.Collections.Frozen.FrozenDictionary<int, DMonster> _all = null!;

    public static DMonster? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DMonster> All()
    {
        return _all.Values;
    }
}
