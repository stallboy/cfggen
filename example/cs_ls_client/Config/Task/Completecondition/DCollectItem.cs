namespace Config.Task.Completecondition
{

public partial class DCollectItem : Task.DCompletecondition
{
    public Task.DCompleteConditionType type() {
        return Task.DCompleteConditionType.CollectItem;
    }

    public int Itemid { get; init; }
    public int Count { get; init; }
}
}
