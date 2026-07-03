using System;
using System.Collections.Generic;
namespace Config.Task
{

public partial class DTaskextraexp
{
    public int Taskid { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public int Extraexp { get; init; } /* 额外奖励经验 */
    public string Test1 { get; init; } = null!;
    public string Test2 { get; init; } = null!;
    public string Fielda { get; init; } = null!;
    public string Fieldb { get; init; } = null!;
    public string Fieldc { get; init; } = null!;
    public string Fieldd { get; init; } = null!;
    private static IReadOnlyList<DTaskextraexp> _allList = null!;
    
    private static Dictionary<int, DTaskextraexp> _all = null!;

    public static DTaskextraexp? Get(int taskid)
    {
        return _all.GetValueOrDefault(taskid);
    }

    public static IReadOnlyList<DTaskextraexp> All()
    {
        return _allList;
    }
}
}
