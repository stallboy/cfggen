namespace Config.Ai;

public partial class DAiCondition
{
    public required int ID { get; init; }
    public required string Desc { get; init; } /* 描述 */
    public required int FormulaID { get; init; } /* 公式 */
    public required IReadOnlyList<int> ArgIList { get; init; } /* 参数(int)1 */
    public required IReadOnlyList<int> ArgSList { get; init; } /* 参数(string)1 */

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
