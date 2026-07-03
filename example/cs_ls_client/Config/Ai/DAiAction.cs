using System;
using System.Collections.Generic;
namespace Config.Ai
{

public partial class DAiAction
{
    public int ID { get; init; }
    public string Desc { get; init; } = null!; /* 描述 */
    public int FormulaID { get; init; } /* 公式 */
    public List<int> ArgIList { get; init; } = null!; /* 参数(int)1 */
    public List<int> ArgSList { get; init; } = null!; /* 参数(string)1 */
    private static IReadOnlyList<DAiAction> _allList = null!;
    
    private static Dictionary<int, DAiAction> _all = null!;

    public static DAiAction? Get(int iD)
    {
        return _all.GetValueOrDefault(iD);
    }

    public static IReadOnlyList<DAiAction> All()
    {
        return _allList;
    }
}
}
