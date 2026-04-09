namespace Config.Task.Completecondition;

public partial class DConditionAnd : Task.DCompletecondition
{
    public Task.DCompleteconditiontype type() {
        return Task.DCompleteconditiontype.ConditionAnd;
    }

    public required Task.DCompletecondition Cond1 { get; init; }
    public required Task.DCompletecondition Cond2 { get; init; }
}
