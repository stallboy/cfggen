namespace Config.Combat;

public partial class DAbilityUse
{
    public required Combat.DBinding Binding { get; init; }
    public required IReadOnlyList<Combat.DMachineUse> Machines { get; init; }
    public Combat.DAbility RefAbility { get; internal set; } = null!;
}
