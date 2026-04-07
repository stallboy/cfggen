namespace Config.Task;

public partial class DataCompleteconditiontype
{
    public static DataCompleteconditiontype KillMonster { get; private set; } = null!;
    public static DataCompleteconditiontype TalkNpc { get; private set; } = null!;
    public static DataCompleteconditiontype CollectItem { get; private set; } = null!;
    public static DataCompleteconditiontype ConditionAnd { get; private set; } = null!;
    public static DataCompleteconditiontype Chat { get; private set; } = null!;
    public static DataCompleteconditiontype TestNoColumn { get; private set; } = null!;
    public static DataCompleteconditiontype Aa { get; private set; } = null!;

    public required int Id { get; init; } /* 任务完成条件类型（id的范围为1-100） */
    public required string Name { get; init; } /* 程序用名字 */
    
    private static OrderedDictionary<int, DataCompleteconditiontype> _all = [];

    public static DataCompleteconditiontype? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DataCompleteconditiontype> All()
    {
        return _all.Values;
    }
}
