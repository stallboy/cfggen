using System.Collections.Generic;
namespace Config.Task;

public partial class DataTask2
{
    public required int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public required List<Config.Text> Name { get; init; }
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

    public override int GetHashCode()
    {
        return Taskid.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataTask2;
        return o != null && Taskid.Equals(o.Taskid);
    }

    public override string ToString()
    {
        return "(" + Taskid + "," + StringUtil.ToString(Name) + "," + Nexttask + "," + Completecondition + "," + Exp + "," + TestBool + "," + TestString + "," + TestStruct + "," + StringUtil.ToString(TestList) + "," + StringUtil.ToString(TestListStruct) + "," + StringUtil.ToString(TestListInterface) + ")";
    }

    
    private static OrderedDictionary<int, DataTask2> _all = [];

    public static DataTask2? Get(int taskid)
    {
        return _all.GetValueOrDefault(taskid);
    }

    public static IReadOnlyList<DataTask2> All()
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
    internal static DataTask2 _create(Stream os)
    {
        var taskid = os.ReadInt32();
        List<Config.Text> name = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            name.Add(Config.Text._create(os));
        var nexttask = os.ReadInt32();
        var completecondition = Task.DataCompletecondition._create(os);
        var exp = os.ReadInt32();
        var testBool = os.ReadBool();
        var testString = os.ReadStringInPool();
        var testStruct = DataPosition._create(os);
        List<int> testList = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            testList.Add(os.ReadInt32());
        List<DataPosition> testListStruct = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            testListStruct.Add(DataPosition._create(os));
        List<Ai.DataTriggerTick> testListInterface = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            testListInterface.Add(Ai.DataTriggerTick._create(os));
        return new DataTask2 {
            Taskid = taskid,
            Name = name,
            Nexttask = nexttask,
            Completecondition = completecondition,
            Exp = exp,
            TestBool = testBool,
            TestString = testString,
            TestStruct = testStruct,
            TestList = testList,
            TestListStruct = testListStruct,
            TestListInterface = testListInterface,
        };
    }

    internal void _resolve(LoadErrors errors)
    {
        Completecondition._resolve(errors);
        NullableRefTaskid = Task.DataTaskextraexp.Get(Taskid);
        NullableRefNexttask = Task.DataTask.Get(Nexttask);
    }
}
