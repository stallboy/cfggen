namespace Config.Combat;

public partial class DPhasebook
{
    public required string School { get; init; }
    public required int Tier { get; init; }
    public required bool Airborne { get; init; }
    public required string Code { get; init; }
    public required int Edition { get; init; }
    public required Combat.DBookEntry Entry { get; init; }

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
