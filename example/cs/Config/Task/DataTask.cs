using System.Collections.Generic;
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

    public override int GetHashCode()
    {
        return Taskid.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataTask;
        return o != null && Taskid.Equals(o.Taskid);
    }

    public override string ToString()
    {
        return "(" + Taskid + "," + StringUtil.ToString(Name) + "," + Nexttask + "," + Completecondition + "," + Exp + "," + TestDefaultBean + ")";
    }

    
    private static OrderedDictionary<int, DataTask> _all = [];

    public static DataTask? Get(int taskid)
    {
        return _all.GetValueOrDefault(taskid);
    }

    public static IReadOnlyList<DataTask> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(self.Taskid, self);
        }

    }

    internal static void Resolve(LoadErrors errors)
    {
        foreach (var v in All())
            v._resolve(errors);
    }
    internal static DataTask _create(Stream os)
    {
        var taskid = os.ReadInt32();
        List<string> name = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            name.Add(os.ReadTextInPool());
        var nexttask = os.ReadInt32();
        var completecondition = Task.DataCompletecondition._create(os);
        var exp = os.ReadInt32();
        var testDefaultBean = Task.DataTestDefaultBean._create(os);
        return new DataTask {
            Taskid = taskid,
            Name = name,
            Nexttask = nexttask,
            Completecondition = completecondition,
            Exp = exp,
            TestDefaultBean = testDefaultBean,
        };
    }

    internal void _resolve(LoadErrors errors)
    {
        Completecondition._resolve(errors);
        NullableRefTaskid = Task.DataTaskextraexp.Get(Taskid);
        NullableRefNexttask = Task.DataTask.Get(Nexttask);
    }
}
