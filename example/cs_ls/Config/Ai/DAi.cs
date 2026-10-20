namespace Config.Ai;

public partial class DAi
{
    public required int ID { get; init; }
    public required string Desc { get; init; } /* 描述----这里测试下多行效果--再来一行 */
    public required string CondID { get; init; } /* 触发公式 */
    public required Ai.DTriggerTick TrigTick { get; init; } /* 触发间隔(帧) */
    public required int TrigOdds { get; init; } /* 触发几率 */
    public required IReadOnlyList<int> ActionID { get; init; } /* 触发行为 */
    public required bool DeathRemove { get; init; } /* 死亡移除 */

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
