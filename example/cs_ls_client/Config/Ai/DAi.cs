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
    public IReadOnlyList<int> ActionID { get; init; } = null!; /* 触发行为 */
    public bool DeathRemove { get; init; } /* 死亡移除 */

    public static DAi? Get(int iD)
        => Get(ConfigMgr.GetMgr(), iD);

    public static DAi? Get(ConfigMgr config, int iD)
    {
        var index = config._ai_dai_All;
        return index.TryGetValue(iD, out var result) ? result : null;
    }

    public static IReadOnlyList<DAi> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DAi> All(ConfigMgr config)
        => config._ai_dai_Rows;
}
}
