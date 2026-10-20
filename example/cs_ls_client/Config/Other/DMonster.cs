using System.Collections.Generic;
namespace Config.Other
{

public partial class DMonster
{
    public int Id { get; init; }
    public IReadOnlyList<DPosition> PosList { get; init; } = null!;
    public int LootId { get; init; } /* loot */
    public int LootItemId { get; init; } /* item */
    public IReadOnlyDictionary<string, int> EnumMap1 { get; init; } = null!;
    public IReadOnlyDictionary<int, string> EnumMap2 { get; init; } = null!;
    public Other.DLootitem RefLoot { get; private set; } = null!;
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
}
