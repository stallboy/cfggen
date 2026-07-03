using System;
using System.Collections.Generic;
namespace Config.Task.Completecondition
{

public partial class DConditionAnd : Task.DCompletecondition
{
    public Task.DCompleteconditiontype type() {
        return Task.DCompleteconditiontype.ConditionAnd;
    }

    public Task.DCompletecondition Cond1 { get; init; } = null!;
    public Task.DCompletecondition Cond2 { get; init; } = null!;
}
}
