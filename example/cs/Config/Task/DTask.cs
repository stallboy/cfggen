namespace Config.Task;

public partial class DTask
{
    public required int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public required IReadOnlyList<string> Name { get; init; } /* 程序用名字 */
    public required int Nexttask { get; init; }
    public required Task.DCompletecondition Completecondition { get; init; }
    public required int Exp { get; init; }
    public required Task.DTestDefaultBean TestDefaultBean { get; init; } /* 测试 */
    public Task.DTaskextraexp? NullableRefTaskid { get; private set; }
    public Task.DTask? NullableRefNexttask { get; private set; }

    public static DTask? Get(int taskid)
        => Get(ConfigMgr.GetMgr(), taskid);

    public static DTask? Get(ConfigMgr config, int taskid)
    {
        var index = config._task_dtask_All;
        return index.TryGetValue(taskid, out var result) ? result : null;
    }

    public static IReadOnlyList<DTask> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DTask> All(ConfigMgr config)
        => config._task_dtask_Rows;
}
