namespace Config.Ai;

public partial class DAiCondition
{
    public required int ID { get; init; }
    public required string Desc { get; init; } /* 描述 */
    public required int FormulaID { get; init; } /* 公式 */
    public required List<int> ArgIList { get; init; } /* 参数(int)1 */
    public required List<int> ArgSList { get; init; } /* 参数(string)1 */
    
    private static System.Collections.Frozen.FrozenDictionary<int, DAiCondition> _all = null!;

    public static DAiCondition? Get(int iD)
    {
        return _all.GetValueOrDefault(iD);
    }

    public static IReadOnlyList<DAiCondition> All()
    {
        return _all.Values;
    }
}
