namespace Config.Task.Completecondition;

public partial class DataKillMonster : Task.DataCompletecondition
{
    public Task.DataCompleteconditiontype type() {
        return Task.DataCompleteconditiontype.KillMonster;
    }

    public required int Monsterid { get; init; }
    public required int Count { get; init; }
    public Other.DataMonster RefMonsterid { get; private set; } = null!;
}
