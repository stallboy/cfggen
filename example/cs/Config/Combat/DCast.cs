namespace Config.Combat;

public partial class DCast
{
    public required int Id { get; init; }
    public required string AbilityCode { get; init; }
    public required Combat.DOrigin Origin { get; init; }
    public required IReadOnlyList<Combat.DAbilityUse> Uses { get; init; }
    public required IReadOnlyList<Combat.DOptionalAim> Optional { get; init; }
    public required string School { get; init; }
    public required int Tier { get; init; }
    public required bool Airborne { get; init; }
    public required string BookCode { get; init; }
    public required int Edition { get; init; }
    public required Combat.DPhaseKey RewardKey { get; init; }
    public Combat.DBookEntry RefBook { get; internal set; } = null!;
    public Combat.DBookEntry RefBookByCode { get; internal set; } = null!;
    public Combat.DPhasebook RefBookRecord { get; internal set; } = null!;
    public Combat.DReward RefReward { get; internal set; } = null!;

    public static DCast? Get(int id)
        => Get(ConfigMgr.GetMgr(), id);

    public static DCast? Get(ConfigMgr config, int id)
    {
        var index = config._combat_dcast_All;
        return index.TryGetValue(id, out var result) ? result : null;
    }

    public static IReadOnlyList<DCast> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DCast> All(ConfigMgr config)
        => config._combat_dcast_Rows;
}
