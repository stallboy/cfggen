namespace Config.Task;

public partial class DTask
{
    public required int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public required List<string> Name { get; init; } /* 程序用名字 */
    public required int Nexttask { get; init; }
    public required Task.DCompletecondition Completecondition { get; init; }
    public required int Exp { get; init; }
    public required Task.DTestDefaultBean TestDefaultBean { get; init; } /* 测试 */
    public Task.DTaskextraexp? NullableRefTaskid { get; private set; }
    public Task.DTask? NullableRefNexttask { get; private set; }
    
    private static System.Collections.Frozen.FrozenDictionary<int, DTask> _all = null!;

    public static DTask? Get(int taskid)
    {
        return _all.GetValueOrDefault(taskid);
    }

    public static IReadOnlyList<DTask> All()
    {
        return _all.Values;
    }
}
