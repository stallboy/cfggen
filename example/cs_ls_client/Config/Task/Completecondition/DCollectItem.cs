using System;
using System.Collections.Generic;
namespace Config.Task.Completecondition
{

public partial class DCollectItem : Task.DCompletecondition
{
    public Task.DCompleteconditiontype type() {
        return Task.DCompleteconditiontype.CollectItem;
    }

    public int Itemid { get; init; }
    public int Count { get; init; }
}
}
