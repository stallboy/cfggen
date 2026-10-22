namespace Config.Combat
{

public partial class DOptionalAim
{
    public int AbilityId { get; init; }
    public string MachineKey { get; init; } = null!;
    public string PhaseKey { get; init; } = null!;
    public Combat.DPhase? NullableRefTarget { get; internal set; }
}
}
