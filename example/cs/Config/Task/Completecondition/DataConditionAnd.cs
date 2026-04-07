namespace Config.Task.Completecondition;

public partial class DataConditionAnd : Task.DataCompletecondition
{
    public Task.DataCompleteconditiontype type() {
        return Task.DataCompleteconditiontype.ConditionAnd;
    }

    public required Task.DataCompletecondition Cond1 { get; init; }
    public required Task.DataCompletecondition Cond2 { get; init; }
}
