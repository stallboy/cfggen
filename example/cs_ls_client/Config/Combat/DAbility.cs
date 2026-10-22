using System.Collections.Generic;
namespace Config.Combat
{

public partial class DAbility
{
    public int Id { get; init; }
    public string Code { get; init; } = null!;
    public Combat.DOrigin Origin { get; init; } = null!;
    public IReadOnlyList<Combat.DMachine> Machines { get; init; } = null!;
    public string StartMachine { get; init; } = null!;
    public string StartPhase { get; init; } = null!;
    public Combat.DPhase RefInitial { get; internal set; } = null!;
    public Combat.DOrigin RefSpawn { get; internal set; } = null!;

    public static DAbility? Get(int id)
        => Get(ConfigMgr.GetMgr(), id);

    public static DAbility? Get(ConfigMgr config, int id)
    {
        var index = config._combat_dability_All;
        return index.TryGetValue(id, out var result) ? result : null;
    }


    public static DAbility? GetByCode(string code)
        => GetByCode(ConfigMgr.GetMgr(), code);

    public static DAbility? GetByCode(ConfigMgr config, string code)
    {
        var index = config._combat_dability_CodeMap;
        return index.TryGetValue(code, out var result) ? result : null;
    }

    public static IReadOnlyList<DAbility> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DAbility> All(ConfigMgr config)
        => config._combat_dability_Rows;
}
}
