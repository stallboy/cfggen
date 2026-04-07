namespace Config.Task.Completecondition;

public partial class DataCollectItem : Task.DataCompletecondition
{
    public Task.DataCompleteconditiontype type() {
        return Task.DataCompleteconditiontype.CollectItem;
    }

    public required int Itemid { get; init; }
    public required int Count { get; init; }
}
