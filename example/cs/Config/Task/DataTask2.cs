namespace Config.Task;

public partial class DataTask2
{
    public required int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public required List<string> Name { get; init; }
    public required int Nexttask { get; init; }
    public required Task.DataCompletecondition Completecondition { get; init; }
    public required int Exp { get; init; }
    public required bool TestBool { get; init; }
    public required string TestString { get; init; }
    public required DataPosition TestStruct { get; init; }
    public required List<int> TestList { get; init; }
    public required List<DataPosition> TestListStruct { get; init; }
    public required List<Ai.DataTriggerTick> TestListInterface { get; init; }
    public Task.DataTaskextraexp? NullableRefTaskid { get; private set; }
    public Task.DataTask? NullableRefNexttask { get; private set; }
    
    private static OrderedDictionary<int, DataTask2> _all = [];

    public static DataTask2? Get(int taskid)
    {
        return _all.GetValueOrDefault(taskid);
    }

    public static IReadOnlyList<DataTask2> All()
    {
        return _all.Values;
    }
}
