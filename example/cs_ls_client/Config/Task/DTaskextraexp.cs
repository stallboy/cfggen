namespace Config.Task;

public partial class DTaskextraexp
{
    public required int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public required int Extraexp { get; init; } /* 额外奖励经验 */
    public required string Test1 { get; init; }
    public required string Test2 { get; init; }
    public required string Fielda { get; init; }
    public required string Fieldb { get; init; }
    public required string Fieldc { get; init; }
    public required string Fieldd { get; init; }
    
    private static System.Collections.Frozen.FrozenDictionary<int, DTaskextraexp> _all = null!;

    public static DTaskextraexp? Get(int taskid)
    {
        return _all.GetValueOrDefault(taskid);
    }

    public static IReadOnlyList<DTaskextraexp> All()
    {
        return _all.Values;
    }
}
