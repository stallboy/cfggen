namespace Config.Other;

public partial class DMonster
{
    public required int Id { get; init; }
    public required IReadOnlyList<DPosition> PosList { get; init; }
    public required int LootId { get; init; } /* loot */
    public required int LootItemId { get; init; } /* item */
    public required IReadOnlyDictionary<string, int> EnumMap1 { get; init; }
    public required IReadOnlyDictionary<int, string> EnumMap2 { get; init; }
    public Other.DLootItem RefLoot { get; private set; } = null!;
    public Other.DLoot RefAllLoot { get; private set; } = null!;
    public IReadOnlyDictionary<int, Other.DArgCaptureMode> RefEnumMap2 { get; private set; } = null!;

    public static DMonster? Get(int id)
        => Get(ConfigMgr.GetMgr(), id);

    public static DMonster? Get(ConfigMgr config, int id)
    {
        var index = config._other_dmonster_All;
        return index.TryGetValue(id, out var result) ? result : null;
    }

    public static IReadOnlyList<DMonster> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DMonster> All(ConfigMgr config)
        => config._other_dmonster_Rows;
}
