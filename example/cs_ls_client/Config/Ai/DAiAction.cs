using System.Collections.Generic;
namespace Config.Ai
{

public partial class DAiAction
{
    public int ID { get; init; }
    public string Desc { get; init; } = null!; /* 描述 */
    public int FormulaID { get; init; } /* 公式 */
    public IReadOnlyList<int> ArgIList { get; init; } = null!; /* 参数(int)1 */
    public IReadOnlyList<int> ArgSList { get; init; } = null!; /* 参数(string)1 */

    public static DAiAction? Get(int iD)
        => Get(ConfigMgr.GetMgr(), iD);

    public static DAiAction? Get(ConfigMgr config, int iD)
    {
        var index = config._ai_dai_uaction_All;
        return index.TryGetValue(iD, out var result) ? result : null;
    }

    public static IReadOnlyList<DAiAction> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DAiAction> All(ConfigMgr config)
        => config._ai_dai_uaction_Rows;
}
}
