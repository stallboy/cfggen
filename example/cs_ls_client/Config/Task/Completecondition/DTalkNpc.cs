using System;
using System.Collections.Generic;
namespace Config.Task.Completecondition
{

public partial class DTalkNpc : Task.DCompletecondition
{
    public Task.DCompleteconditiontype type() {
        return Task.DCompleteconditiontype.TalkNpc;
    }

    public int Npcid { get; init; }
}
}
