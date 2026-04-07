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
    
    private static OrderedDictionary<int, DataMonster> _all = [];

    public static DataMonster? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DataMonster> All()
    {
        return _all.Values;
    }
}
