using System.Collections.Generic;
namespace Config.Combat.Command
{

public partial class DStateMachine : Combat.DCommand
{
    public IReadOnlyList<Combat.DNestedPhase> Phases { get; init; } = null!;
}
}
