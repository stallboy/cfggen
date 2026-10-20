namespace Config.Task.Completecondition;

public partial class DCollectItem : Task.DCompletecondition
{
    public Task.DCompleteConditionType type() {
        return Task.DCompleteConditionType.CollectItem;
    }

    public required int Itemid { get; init; }
    public required int Count { get; init; }
}
