using System;
using System.Collections.Generic;
namespace Config.Task.Completecondition
{

public partial class DChat : Task.DCompletecondition
{
    public Task.DCompleteconditiontype type() {
        return Task.DCompleteconditiontype.Chat;
    }

    public string Msg { get; init; } = null!;
}
}
