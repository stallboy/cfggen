namespace Config.Task;

public enum DCompleteconditiontype
{
    KillMonster,
    TalkNpc,
    CollectItem,
    ConditionAnd,
    Chat,
    TestNoColumn,
    Aa,
}

public partial class DCompleteconditiontypeInfo
{
    public required int Id { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public required string Name { get; init; } /* 程序用名字 */
    public required DCompleteconditiontype eEnum { get; init; }
    
    private static System.Collections.Frozen.FrozenDictionary<int, DCompleteconditiontypeInfo> _all = null!;

    public static DCompleteconditiontypeInfo? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DCompleteconditiontypeInfo> All()
    {
        return _all.Values;
    }
}

public static class DCompleteconditiontypeExtensions
{
    internal static readonly DCompleteconditiontypeInfo[] _infos = new DCompleteconditiontypeInfo[7];

    public static DCompleteconditiontypeInfo Info(this DCompleteconditiontype e)
    {
        return _infos[(int)e];
    }
}
