using System.Collections.Generic;
namespace Config.Task
{

public partial class DTask2
{
    public int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public IReadOnlyList<Config.Text> Name { get; init; } = null!;
    public int Nexttask { get; init; }
    public Task.DCompletecondition Completecondition { get; init; } = null!;
    public int Exp { get; init; }
    public bool TestBool { get; init; }
    public string TestString { get; init; } = null!;
    public DPosition TestStruct { get; init; } = null!;
    public IReadOnlyList<int> TestList { get; init; } = null!;
    public IReadOnlyList<DPosition> TestListStruct { get; init; } = null!;
    public IReadOnlyList<Ai.DTriggerTick> TestListInterface { get; init; } = null!;
    public Task.DTaskextraexp? NullableRefTaskid { get; private set; }
    public Task.DTask? NullableRefNexttask { get; private set; }

    public static DTask2? Get(int taskid)
        => Get(ConfigMgr.GetMgr(), taskid);

    public static DTask2? Get(ConfigMgr config, int taskid)
    {
        var index = config._task_dtask2_All;
        return index.TryGetValue(taskid, out var result) ? result : null;
    }

    public static IReadOnlyList<DTask2> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DTask2> All(ConfigMgr config)
        => config._task_dtask2_Rows;
}
}
