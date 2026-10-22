namespace Config.Combat;

public partial class DTimeline
{
    public required int Id { get; init; }
    public required IReadOnlyDictionary<Combat.DPhaseKey, Combat.DEffect> Events { get; init; }
    public required IReadOnlyDictionary<string, Combat.DEffect> Channels { get; init; }
    public required IReadOnlyDictionary<int, Combat.DEffect> Slots { get; init; }
    public required IReadOnlyDictionary<long, Combat.DEffect> Clock { get; init; }
    public required IReadOnlyDictionary<bool, Combat.DEffect> Modes { get; init; }
    public required Combat.DPhaseKey EventKey { get; init; }
    public required string Channel { get; init; }
    public required int Slot { get; init; }
    public required long Frame { get; init; }
    public required bool Airborne { get; init; }
    public required Combat.DEffect Fallback { get; init; }
    public Combat.DEffect RefEventKey { get; internal set; } = null!;
    public Combat.DEffect RefChannel { get; internal set; } = null!;
    public Combat.DEffect RefSlot { get; internal set; } = null!;
    public Combat.DEffect RefFrame { get; internal set; } = null!;
    public Combat.DEffect RefAirborne { get; internal set; } = null!;
    public Combat.DEffect RefDefault { get; internal set; } = null!;

    public static DTimeline? Get(int id)
        => Get(ConfigMgr.GetMgr(), id);

    public static DTimeline? Get(ConfigMgr config, int id)
    {
        var index = config._combat_dtimeline_All;
        return index.TryGetValue(id, out var result) ? result : null;
    }

    public static IReadOnlyList<DTimeline> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DTimeline> All(ConfigMgr config)
        => config._combat_dtimeline_Rows;
}
