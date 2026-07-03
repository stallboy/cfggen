using System;
using System.Collections.Generic;
namespace Config.Task
{

public partial class DTask
{
    public int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public List<Config.Text> Name { get; init; } = null!; /* 程序用名字 */
    public int Nexttask { get; init; }
    public Task.DCompletecondition Completecondition { get; init; } = null!;
    public int Exp { get; init; }
    public Task.DTestDefaultBean TestDefaultBean { get; init; } = null!; /* 测试 */
    public Task.DTaskextraexp? NullableRefTaskid { get; private set; }
    public Task.DTask? NullableRefNexttask { get; private set; }
    private static IReadOnlyList<DTask> _allList = null!;
    
    private static Dictionary<int, DTask> _all = null!;

    public static DTask? Get(int taskid)
    {
        return _all.GetValueOrDefault(taskid);
    }

    public static IReadOnlyList<DTask> All()
    {
        return _allList;
    }
}
}
