namespace Config.Task.Completecondition;

public partial class DataTalkNpc : Task.DataCompletecondition
{
    public Task.DataCompleteconditiontype type() {
        return Task.DataCompleteconditiontype.TalkNpc;
    }

    public required int Npcid { get; init; }
}
