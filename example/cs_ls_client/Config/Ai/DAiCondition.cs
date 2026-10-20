using System.Collections.Generic;
namespace Config.Ai
{

public partial class DAiCondition
{
    public int ID { get; init; }
    public string Desc { get; init; } = null!; /* 描述 */
    public int FormulaID { get; init; } /* 公式 */
    public IReadOnlyList<int> ArgIList { get; init; } = null!; /* 参数(int)1 */
    public IReadOnlyList<int> ArgSList { get; init; } = null!; /* 参数(string)1 */

    public static DAiCondition? Get(int iD)
        => Get(ConfigMgr.GetMgr(), iD);

    public static DAiCondition? Get(ConfigMgr config, int iD)
    {
        var index = config._ai_dai_ucondition_All;
        return index.TryGetValue(iD, out var result) ? result : null;
    }

    public static IReadOnlyList<DAiCondition> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DAiCondition> All(ConfigMgr config)
        => config._ai_dai_ucondition_Rows;
}
}
