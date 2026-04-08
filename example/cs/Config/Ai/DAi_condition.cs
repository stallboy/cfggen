namespace Config.Ai;

public partial class DAi_condition
{
    public required int ID { get; init; }
    public required string Desc { get; init; } /* 描述 */
    public required int FormulaID { get; init; } /* 公式 */
    public required List<int> ArgIList { get; init; } /* 参数(int)1 */
    public required List<int> ArgSList { get; init; } /* 参数(string)1 */
    
    private static OrderedDictionary<int, DAi_condition> _all = [];

    public static DAi_condition? Get(int iD)
    {
        return _all.GetValueOrDefault(iD);
    }

    public static IReadOnlyList<DAi_condition> All()
    {
        return _all.Values;
    }
}
