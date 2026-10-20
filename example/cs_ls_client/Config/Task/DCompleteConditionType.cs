using System.Collections.Generic;
namespace Config.Task
{

public enum DCompleteconditiontype
{
    KillMonster,
    TalkNpc,
    CollectItem,
    ConditionAnd,
    Chat,
    TestNoColumn,
    Aa,
}

public partial class DCompleteconditiontypeInfo
{
    public int Id { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public string Name { get; init; } = null!; /* 程序用名字 */
    public DCompleteconditiontype EEnum { get; init; }

    public static DCompleteconditiontypeInfo? Get(int id)
        => Get(ConfigMgr.GetMgr(), id);

    public static DCompleteconditiontypeInfo? Get(ConfigMgr config, int id)
    {
        var index = config._task_dcompleteconditiontype_All;
        return index.TryGetValue(id, out var result) ? result : null;
    }

    public static IReadOnlyList<DCompleteconditiontypeInfo> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DCompleteconditiontypeInfo> All(ConfigMgr config)
        => config._task_dcompleteconditiontype_Rows;
}

public static class DCompleteconditiontypeExtensions
{
    public static DCompleteconditiontypeInfo Info(this DCompleteconditiontype e)
        => e.Info(ConfigMgr.GetMgr());

    public static DCompleteconditiontypeInfo Info(this DCompleteconditiontype e, ConfigMgr config)
        => config._task_dcompleteconditiontype_Symbols[(int)e]!;
}
}
