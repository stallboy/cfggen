using System.Collections.Generic;
namespace Config.Combat
{

public partial class DMachineUse
{
    public string MachineKey { get; init; } = null!;
    public IReadOnlyList<Combat.DPhaseUse> Targets { get; init; } = null!;
}
}
