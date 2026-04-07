namespace Config.Task.Completecondition;

public partial class DataChat : Task.DataCompletecondition
{
    public Task.DataCompleteconditiontype type() {
        return Task.DataCompleteconditiontype.Chat;
    }

    public required string Msg { get; init; }
}
