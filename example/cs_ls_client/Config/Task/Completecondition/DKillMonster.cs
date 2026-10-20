namespace Config.Task.Completecondition
{

public partial class DKillMonster : Task.DCompletecondition
{
    public Task.DCompleteConditionType type() {
        return Task.DCompleteConditionType.KillMonster;
    }

    public int Monsterid { get; init; }
    public int Count { get; init; }
    public Other.DMonster RefMonsterid { get; private set; } = null!;
}
}
