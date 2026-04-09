namespace Config.Task;

public partial class DTask2
{
    public required int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public required List<Config.Text> Name { get; init; }
    public required int Nexttask { get; init; }
    public required Task.DCompletecondition Completecondition { get; init; }
    public required int Exp { get; init; }
    public required bool TestBool { get; init; }
    public required string TestString { get; init; }
    public required DPosition TestStruct { get; init; }
    public required List<int> TestList { get; init; }
    public required List<DPosition> TestListStruct { get; init; }
    public required List<Ai.DTriggerTick> TestListInterface { get; init; }
    public Task.DTaskextraexp? NullableRefTaskid { get; private set; }
    public Task.DTask? NullableRefNexttask { get; private set; }
    
    private static System.Collections.Frozen.FrozenDictionary<int, DTask2> _all = null!;

    public static DTask2? Get(int taskid)
    {
        return _all.GetValueOrDefault(taskid);
    }

    public static IReadOnlyList<DTask2> All()
    {
        return _all.Values;
    }
}
