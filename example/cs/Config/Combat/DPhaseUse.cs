namespace Config.Combat;

public partial class DPhaseUse
{
    public required string PhaseKey { get; init; }
    public Combat.DPhase RefTarget { get; internal set; } = null!;
    public Combat.DPhase RefByCode { get; internal set; } = null!;
    public Combat.DOrigin RefOrigin { get; internal set; } = null!;
}
