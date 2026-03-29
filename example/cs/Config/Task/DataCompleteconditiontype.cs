using System.Collections.Generic;
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

    public override int GetHashCode()
    {
        return Id.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataCompleteconditiontype;
        return o != null && Id.Equals(o.Id);
    }

    public override string ToString()
    {
        return "(" + Id + "," + Name + ")";
    }

    
    private static OrderedDictionary<int, DataCompleteconditiontype> _all = [];

    public static DataCompleteconditiontype? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DataCompleteconditiontype> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(self.Id, self);
            if (self.Name.Trim().Length == 0)
                continue;
            switch(self.Name.Trim())
            {
                case "KillMonster":
                    if (KillMonster != null)
                        errors.EnumDup("task.completeconditiontype", self.ToString());
                    KillMonster = self;
                    break;
                case "TalkNpc":
                    if (TalkNpc != null)
                        errors.EnumDup("task.completeconditiontype", self.ToString());
                    TalkNpc = self;
                    break;
                case "CollectItem":
                    if (CollectItem != null)
                        errors.EnumDup("task.completeconditiontype", self.ToString());
                    CollectItem = self;
                    break;
                case "ConditionAnd":
                    if (ConditionAnd != null)
                        errors.EnumDup("task.completeconditiontype", self.ToString());
                    ConditionAnd = self;
                    break;
                case "Chat":
                    if (Chat != null)
                        errors.EnumDup("task.completeconditiontype", self.ToString());
                    Chat = self;
                    break;
                case "TestNoColumn":
                    if (TestNoColumn != null)
                        errors.EnumDup("task.completeconditiontype", self.ToString());
                    TestNoColumn = self;
                    break;
                case "aa":
                    if (Aa != null)
                        errors.EnumDup("task.completeconditiontype", self.ToString());
                    Aa = self;
                    break;
                default:
                    errors.EnumDataAdd("task.completeconditiontype", self.ToString());
                    break;
            }
        }

        if (KillMonster == null)
            errors.EnumNull("task.completeconditiontype", "KillMonster");
        if (TalkNpc == null)
            errors.EnumNull("task.completeconditiontype", "TalkNpc");
        if (CollectItem == null)
            errors.EnumNull("task.completeconditiontype", "CollectItem");
        if (ConditionAnd == null)
            errors.EnumNull("task.completeconditiontype", "ConditionAnd");
        if (Chat == null)
            errors.EnumNull("task.completeconditiontype", "Chat");
        if (TestNoColumn == null)
            errors.EnumNull("task.completeconditiontype", "TestNoColumn");
        if (Aa == null)
            errors.EnumNull("task.completeconditiontype", "aa");
    }

    internal static DataCompleteconditiontype _create(Stream os)
    {
        var id = os.ReadInt32();
        var name = os.ReadStringInPool();
        return new DataCompleteconditiontype {
            Id = id,
            Name = name,
        };
    }

}
