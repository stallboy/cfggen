using System.Collections.Generic;
namespace Config.Ai;

public partial class DataAi
{
    public required int ID { get; init; }
    public required string Desc { get; init; } /* 描述----这里测试下多行效果--再来一行 */
    public required string CondID { get; init; } /* 触发公式 */
    public required Ai.DataTriggerTick TrigTick { get; init; } /* 触发间隔(帧) */
    public required int TrigOdds { get; init; } /* 触发几率 */
    public required List<int> ActionID { get; init; } /* 触发行为 */
    public required bool DeathRemove { get; init; } /* 死亡移除 */

    public override int GetHashCode()
    {
        return ID.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataAi;
        return o != null && ID.Equals(o.ID);
    }

    public override string ToString()
    {
        return "(" + ID + "," + Desc + "," + CondID + "," + TrigTick + "," + TrigOdds + "," + StringUtil.ToString(ActionID) + "," + DeathRemove + ")";
    }

    
    private static OrderedDictionary<int, DataAi> _all = [];

    public static DataAi? Get(int iD)
    {
        return _all.GetValueOrDefault(iD);
    }

    public static IReadOnlyList<DataAi> All()
    {
        return _all.Values;
    }

    internal static void Initialize(Stream os, LoadErrors errors)
    {
        _all = [];
        for (var c = os.ReadInt32(); c > 0; c--)
        {
            var self = _create(os);
            _all.Add(self.ID, self);
        }

    }

    internal static DataAi _create(Stream os)
    {
        var iD = os.ReadInt32();
        var desc = os.ReadStringInPool();
        var condID = os.ReadStringInPool();
        var trigTick = Ai.DataTriggerTick._create(os);
        var trigOdds = os.ReadInt32();
        List<int> actionID = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            actionID.Add(os.ReadInt32());
        var deathRemove = os.ReadBool();
        return new DataAi {
            ID = iD,
            Desc = desc,
            CondID = condID,
            TrigTick = trigTick,
            TrigOdds = trigOdds,
            ActionID = actionID,
            DeathRemove = deathRemove,
        };
    }

}
