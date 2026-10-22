using System.Collections.Generic;
namespace Config.Combat
{

public partial class DAbilityUse
{
    public Combat.DBinding Binding { get; init; } = null!;
    public IReadOnlyList<Combat.DMachineUse> Machines { get; init; } = null!;
    public Combat.DAbility RefAbility { get; internal set; } = null!;
}
}
