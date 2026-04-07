namespace Config.Task;

public partial class DataTask
{
    public required int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public required List<string> Name { get; init; } /* 程序用名字 */
    public required int Nexttask { get; init; }
    public required Task.DataCompletecondition Completecondition { get; init; }
    public required int Exp { get; init; }
    public required Task.DataTestDefaultBean TestDefaultBean { get; init; } /* 测试 */
    public Task.DataTaskextraexp? NullableRefTaskid { get; private set; }
    public Task.DataTask? NullableRefNexttask { get; private set; }
    
    private static OrderedDictionary<int, DataTask> _all = [];

    public static DataTask? Get(int taskid)
    {
        return _all.GetValueOrDefault(taskid);
    }

    public static IReadOnlyList<DataTask> All()
    {
        return _all.Values;
    }
}
