using System;
using System.Collections.Generic;
namespace Config.Task.Completecondition
{

public partial class DKillMonster : Task.DCompletecondition
{
    public Task.DCompleteconditiontype type() {
        return Task.DCompleteconditiontype.KillMonster;
    }

    public int Monsterid { get; init; }
    public int Count { get; init; }
    public Other.DMonster RefMonsterid { get; private set; } = null!;
}
}
