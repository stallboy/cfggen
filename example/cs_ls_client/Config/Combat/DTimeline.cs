using System.Collections.Generic;
namespace Config.Combat
{

public partial class DTimeline
{
    public int Id { get; init; }
    public IReadOnlyDictionary<Combat.DPhaseKey, Combat.DEffect> Events { get; init; } = null!;
    public IReadOnlyDictionary<string, Combat.DEffect> Channels { get; init; } = null!;
    public IReadOnlyDictionary<int, Combat.DEffect> Slots { get; init; } = null!;
    public IReadOnlyDictionary<long, Combat.DEffect> Clock { get; init; } = null!;
    public IReadOnlyDictionary<bool, Combat.DEffect> Modes { get; init; } = null!;
    public Combat.DPhaseKey EventKey { get; init; } = null!;
    public string Channel { get; init; } = null!;
    public int Slot { get; init; }
    public long Frame { get; init; }
    public bool Airborne { get; init; }
    public Combat.DEffect Fallback { get; init; } = null!;
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
}
