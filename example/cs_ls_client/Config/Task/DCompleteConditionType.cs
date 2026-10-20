using System.Collections.Generic;
namespace Config.Task
{

public enum DCompleteConditionType
{
    KillMonster,
    TalkNpc,
    CollectItem,
    ConditionAnd,
    Chat,
    TestNoColumn,
    Aa,
}

public partial class DCompleteConditionTypeInfo
{
    public int Id { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public string Name { get; init; } = null!; /* 程序用名字 */
    public DCompleteConditionType EEnum { get; init; }

    public static DCompleteConditionTypeInfo? Get(int id)
        => Get(ConfigMgr.GetMgr(), id);

    public static DCompleteConditionTypeInfo? Get(ConfigMgr config, int id)
    {
        var index = config._task_dcomplete_ucondition_utype_All;
        return index.TryGetValue(id, out var result) ? result : null;
    }

    public static IReadOnlyList<DCompleteConditionTypeInfo> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DCompleteConditionTypeInfo> All(ConfigMgr config)
        => config._task_dcomplete_ucondition_utype_Rows;
}

public static class DCompleteConditionTypeExtensions
{
    public static DCompleteConditionTypeInfo Info(this DCompleteConditionType e)
        => e.Info(ConfigMgr.GetMgr());

    public static DCompleteConditionTypeInfo Info(this DCompleteConditionType e, ConfigMgr config)
        => config._task_dcomplete_ucondition_utype_Symbols[(int)e]!;
}
}
