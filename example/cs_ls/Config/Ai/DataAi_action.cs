using System.Collections.Generic;
namespace Config.Ai;

public partial class DataAi_action
{
    public required int ID { get; init; }
    public required string Desc { get; init; } /* 描述 */
    public required int FormulaID { get; init; } /* 公式 */
    public required List<int> ArgIList { get; init; } /* 参数(int)1 */
    public required List<int> ArgSList { get; init; } /* 参数(string)1 */

    public override int GetHashCode()
    {
        return ID.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataAi_action;
        return o != null && ID.Equals(o.ID);
    }

    public override string ToString()
    {
        return "(" + ID + "," + Desc + "," + FormulaID + "," + StringUtil.ToString(ArgIList) + "," + StringUtil.ToString(ArgSList) + ")";
    }

    
    private static OrderedDictionary<int, DataAi_action> _all = [];

    public static DataAi_action? Get(int iD)
    {
        return _all.GetValueOrDefault(iD);
    }

    public static IReadOnlyList<DataAi_action> All()
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

    internal static DataAi_action _create(Stream os)
    {
        var iD = os.ReadInt32();
        var desc = os.ReadStringInPool();
        var formulaID = os.ReadInt32();
        List<int> argIList = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            argIList.Add(os.ReadInt32());
        List<int> argSList = [];
        for (var c = os.ReadInt32(); c > 0; c--)
            argSList.Add(os.ReadInt32());
        return new DataAi_action {
            ID = iD,
            Desc = desc,
            FormulaID = formulaID,
            ArgIList = argIList,
            ArgSList = argSList,
        };
    }

}
