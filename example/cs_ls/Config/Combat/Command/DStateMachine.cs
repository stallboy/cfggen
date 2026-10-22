namespace Config.Combat.Command;

public partial class DStateMachine : Combat.DCommand
{
    public required IReadOnlyList<Combat.DNestedPhase> Phases { get; init; }
}
