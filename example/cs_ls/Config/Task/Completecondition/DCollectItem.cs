namespace Config.Task.Completecondition;

public partial class DCollectItem : Task.DCompletecondition
{
    public Task.DCompleteconditiontype type() {
        return Task.DCompleteconditiontype.CollectItem;
    }

    public required int Itemid { get; init; }
    public required int Count { get; init; }
}
