namespace Config.Task.Completecondition;

public partial class DChat : Task.DCompletecondition
{
    public Task.DCompleteconditiontype type() {
        return Task.DCompleteconditiontype.Chat;
    }

    public required string Msg { get; init; }
}
