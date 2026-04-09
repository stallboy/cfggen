namespace Config.Task.Completecondition;

public partial class DTalkNpc : Task.DCompletecondition
{
    public Task.DCompleteconditiontype type() {
        return Task.DCompleteconditiontype.TalkNpc;
    }

    public required int Npcid { get; init; }
}
