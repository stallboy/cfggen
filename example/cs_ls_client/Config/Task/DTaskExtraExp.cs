using System.Collections.Generic;
namespace Config.Task
{

public partial class DTaskExtraExp
{
    public int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public int Extraexp { get; init; } /* 额外奖励经验 */
    public string Test1 { get; init; } = null!;
    public string Test2 { get; init; } = null!;
    public string Fielda { get; init; } = null!;
    public string Fieldb { get; init; } = null!;
    public string Fieldc { get; init; } = null!;
    public string Fieldd { get; init; } = null!;

    public static DTaskExtraExp? Get(int taskid)
        => Get(ConfigMgr.GetMgr(), taskid);

    public static DTaskExtraExp? Get(ConfigMgr config, int taskid)
    {
        var index = config._task_dtask_uextra_uexp_All;
        return index.TryGetValue(taskid, out var result) ? result : null;
    }

    public static IReadOnlyList<DTaskExtraExp> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DTaskExtraExp> All(ConfigMgr config)
        => config._task_dtask_uextra_uexp_Rows;
}
}
