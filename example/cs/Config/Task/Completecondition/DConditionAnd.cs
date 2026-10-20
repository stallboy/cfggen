namespace Config.Task.Completecondition;

public partial class DConditionAnd : Task.DCompletecondition
{
    public Task.DCompleteConditionType type() {
        return Task.DCompleteConditionType.ConditionAnd;
    }

    public required Task.DCompletecondition Cond1 { get; init; }
    public required Task.DCompletecondition Cond2 { get; init; }
}
