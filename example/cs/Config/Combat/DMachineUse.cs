namespace Config.Combat;

public partial class DMachineUse
{
    public required string MachineKey { get; init; }
    public required IReadOnlyList<Combat.DPhaseUse> Targets { get; init; }
}
