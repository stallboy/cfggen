namespace Config.Task.Completecondition
{

public partial class DChat : Task.DCompletecondition
{
    public Task.DCompleteConditionType type() {
        return Task.DCompleteConditionType.Chat;
    }

    public string Msg { get; init; } = null!;
}
}
