namespace Config.Task.Completecondition
{

public partial class DConditionAnd : Task.DCompletecondition
{
    public Task.DCompleteConditionType type() {
        return Task.DCompleteConditionType.ConditionAnd;
    }

    public Task.DCompletecondition Cond1 { get; init; } = null!;
    public Task.DCompletecondition Cond2 { get; init; } = null!;
}
}
