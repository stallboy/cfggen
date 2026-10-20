namespace Config.Ai;

public partial class DAiAction
{
    public required int ID { get; init; }
    public required string Desc { get; init; } /* 描述 */
    public required int FormulaID { get; init; } /* 公式 */
    public required IReadOnlyList<int> ArgIList { get; init; } /* 参数(int)1 */
    public required IReadOnlyList<int> ArgSList { get; init; } /* 参数(string)1 */

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
