namespace Config.Task;

public enum DataCompleteconditiontype
{
    KillMonster,
    TalkNpc,
    CollectItem,
    ConditionAnd,
    Chat,
    TestNoColumn,
    Aa,
}

public partial class DataCompleteconditiontypeInfo
{
    public required int Id { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public required string Name { get; init; } /* 程序用名字 */
    public required DataCompleteconditiontype eEnum { get; init; }
    
    private static OrderedDictionary<int, DataCompleteconditiontypeInfo> _all = [];

    public static DataCompleteconditiontypeInfo? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DataCompleteconditiontypeInfo> All()
    {
        return _all.Values;
    }
}

public static class DataCompleteconditiontypeExtensions
{
    internal static readonly DataCompleteconditiontypeInfo[] _infos = new DataCompleteconditiontypeInfo[7];

    public static DataCompleteconditiontypeInfo Info(this DataCompleteconditiontype e)
    {
        return _infos[(int)e];
    }
}
