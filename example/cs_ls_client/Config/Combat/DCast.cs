using System.Collections.Generic;
namespace Config.Combat
{

public partial class DCast
{
    public int Id { get; init; }
    public string AbilityCode { get; init; } = null!;
    public Combat.DOrigin Origin { get; init; } = null!;
    public IReadOnlyList<Combat.DAbilityUse> Uses { get; init; } = null!;
    public IReadOnlyList<Combat.DOptionalAim> Optional { get; init; } = null!;
    public string School { get; init; } = null!;
    public int Tier { get; init; }
    public bool Airborne { get; init; }
    public string BookCode { get; init; } = null!;
    public int Edition { get; init; }
    public Combat.DPhaseKey RewardKey { get; init; } = null!;
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
}
