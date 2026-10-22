using System.Collections.Generic;
namespace Config.Combat
{

public partial class DPhasebook
{
    public string School { get; init; } = null!;
    public int Tier { get; init; }
    public bool Airborne { get; init; }
    public string Code { get; init; } = null!;
    public int Edition { get; init; }
    public Combat.DBookEntry Entry { get; init; } = null!;

    public sealed record SchoolTierAirborneKey(string School, int Tier, bool Airborne);

    public static DPhasebook? Get(string school, int tier, bool airborne)
        => Get(ConfigMgr.GetMgr(), school, tier, airborne);

    public static DPhasebook? Get(ConfigMgr config, string school, int tier, bool airborne)
    {
        var index = config._combat_dphasebook_All;
        return index.TryGetValue(new SchoolTierAirborneKey(school, tier, airborne), out var result) ? result : null;
    }


    public sealed record CodeEditionKey(string Code, int Edition);

    public static DPhasebook? GetByCodeEdition(string code, int edition)
        => GetByCodeEdition(ConfigMgr.GetMgr(), code, edition);

    public static DPhasebook? GetByCodeEdition(ConfigMgr config, string code, int edition)
    {
        var index = config._combat_dphasebook_CodeEditionMap;
        return index.TryGetValue(new CodeEditionKey(code, edition), out var result) ? result : null;
    }

    public static IReadOnlyList<DPhasebook> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DPhasebook> All(ConfigMgr config)
        => config._combat_dphasebook_Rows;
}
}
