namespace Config.Combat;

public partial class DOptionalAim
{
    public required int AbilityId { get; init; }
    public required string MachineKey { get; init; }
    public required string PhaseKey { get; init; }
    public Combat.DPhase? NullableRefTarget { get; internal set; }
}
