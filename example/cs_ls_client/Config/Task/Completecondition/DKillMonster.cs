namespace Config.Task.Completecondition;

public partial class DKillMonster : Task.DCompletecondition
{
    public Task.DCompleteconditiontype type() {
        return Task.DCompleteconditiontype.KillMonster;
    }

    public required int Monsterid { get; init; }
    public required int Count { get; init; }
    public Other.DMonster RefMonsterid { get; private set; } = null!;
}
