using System;
using System.Collections.Generic;
namespace Config.Ai
{

public partial class DAi
{
    public int ID { get; init; }
    public string Desc { get; init; } = null!; /* 描述----这里测试下多行效果--再来一行 */
    public string CondID { get; init; } = null!; /* 触发公式 */
    public Ai.DTriggerTick TrigTick { get; init; } = null!; /* 触发间隔(帧) */
    public int TrigOdds { get; init; } /* 触发几率 */
    public List<int> ActionID { get; init; } = null!; /* 触发行为 */
    public bool DeathRemove { get; init; } /* 死亡移除 */
    private static IReadOnlyList<DAi> _allList = null!;
    
    private static Dictionary<int, DAi> _all = null!;

    public static DAi? Get(int iD)
    {
        return _all.GetValueOrDefault(iD);
    }

    public static IReadOnlyList<DAi> All()
    {
        return _allList;
    }
}
}
