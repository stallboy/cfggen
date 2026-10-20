namespace Config.Task.Completecondition
{

public partial class DTalkNpc : Task.DCompletecondition
{
    public Task.DCompleteConditionType type() {
        return Task.DCompleteConditionType.TalkNpc;
    }

    public int Npcid { get; init; }
}
}
