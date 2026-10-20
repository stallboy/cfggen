namespace Config.Task;

public partial class DTask2
{
    public required int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public required IReadOnlyList<Config.Text> Name { get; init; }
    public required int Nexttask { get; init; }
    public required Task.DCompletecondition Completecondition { get; init; }
    public required int Exp { get; init; }
    public required bool TestBool { get; init; }
    public required string TestString { get; init; }
    public required DPosition TestStruct { get; init; }
    public required IReadOnlyList<int> TestList { get; init; }
    public required IReadOnlyList<DPosition> TestListStruct { get; init; }
    public required IReadOnlyList<Ai.DTriggerTick> TestListInterface { get; init; }
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
