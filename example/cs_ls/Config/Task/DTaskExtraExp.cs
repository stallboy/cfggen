namespace Config.Task;

public partial class DTaskExtraExp
{
    public required int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public required int Extraexp { get; init; } /* 额外奖励经验 */
    public required string Test1 { get; init; }
    public required string Test2 { get; init; }
    public required string Fielda { get; init; }
    public required string Fieldb { get; init; }
    public required string Fieldc { get; init; }
    public required string Fieldd { get; init; }

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
