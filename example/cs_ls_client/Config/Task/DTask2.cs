using System;
using System.Collections.Generic;
namespace Config.Task
{

public partial class DTask2
{
    public int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public List<Config.Text> Name { get; init; } = null!;
    public int Nexttask { get; init; }
    public Task.DCompletecondition Completecondition { get; init; } = null!;
    public int Exp { get; init; }
    public bool TestBool { get; init; }
    public string TestString { get; init; } = null!;
    public DPosition TestStruct { get; init; } = null!;
    public List<int> TestList { get; init; } = null!;
    public List<DPosition> TestListStruct { get; init; } = null!;
    public List<Ai.DTriggerTick> TestListInterface { get; init; } = null!;
    public Task.DTaskextraexp? NullableRefTaskid { get; private set; }
    public Task.DTask? NullableRefNexttask { get; private set; }
    private static IReadOnlyList<DTask2> _allList = null!;
    
    private static Dictionary<int, DTask2> _all = null!;

    public static DTask2? Get(int taskid)
    {
        return _all.GetValueOrDefault(taskid);
    }

    public static IReadOnlyList<DTask2> All()
    {
        return _allList;
    }
}
}
