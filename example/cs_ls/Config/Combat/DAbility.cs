namespace Config.Combat;

public partial class DAbility
{
    public required int Id { get; init; }
    public required string Code { get; init; }
    public required Combat.DOrigin Origin { get; init; }
    public required IReadOnlyList<Combat.DMachine> Machines { get; init; }
    public required string StartMachine { get; init; }
    public required string StartPhase { get; init; }
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
