using System.Collections.Generic;
namespace Config.Task;

public partial class DataTaskextraexp
{
    public required int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public required int Extraexp { get; init; } /* 额外奖励经验 */
    public required string Test1 { get; init; }
    public required string Test2 { get; init; }
    public required string Fielda { get; init; }
    public required string Fieldb { get; init; }
    public required string Fieldc { get; init; }
    public required string Fieldd { get; init; }

    public override int GetHashCode()
    {
        return Taskid.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataTaskextraexp;
        return o != null && Taskid.Equals(o.Taskid);
    }

    public override string ToString()
    {
        return "(" + Taskid + "," + Extraexp + "," + Test1 + "," + Test2 + "," + Fielda + "," + Fieldb + "," + Fieldc + "," + Fieldd + ")";
    }

    
    private static OrderedDictionary<int, DataTaskextraexp> _all = [];

    public static DataTaskextraexp? Get(int taskid)
    {
        return _all.GetValueOrDefault(taskid);
    }

    public static IReadOnlyList<DataTaskextraexp> All()
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

    internal static DataTaskextraexp _create(Stream os)
    {
        var taskid = os.ReadInt32();
        var extraexp = os.ReadInt32();
        var test1 = os.ReadStringInPool();
        var test2 = os.ReadStringInPool();
        var fielda = os.ReadStringInPool();
        var fieldb = os.ReadStringInPool();
        var fieldc = os.ReadStringInPool();
        var fieldd = os.ReadStringInPool();
        return new DataTaskextraexp {
            Taskid = taskid,
            Extraexp = extraexp,
            Test1 = test1,
            Test2 = test2,
            Fielda = fielda,
            Fieldb = fieldb,
            Fieldc = fieldc,
            Fieldd = fieldd,
        };
    }

}
