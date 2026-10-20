using System.Collections.Generic;
namespace Config.Task
{

public partial class DTask
{
    public int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public IReadOnlyList<Config.Text> Name { get; init; } = null!; /* 程序用名字 */
    public int Nexttask { get; init; }
    public Task.DCompletecondition Completecondition { get; init; } = null!;
    public int Exp { get; init; }
    public Task.DTestDefaultBean TestDefaultBean { get; init; } = null!; /* 测试 */
    public Task.DTaskExtraExp? NullableRefTaskid { get; private set; }
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
}
