namespace Config.Ai;

public partial class DataAi
{
    public required int ID { get; init; }
    public required string Desc { get; init; } /* 描述----这里测试下多行效果--再来一行 */
    public required string CondID { get; init; } /* 触发公式 */
    public required Ai.DataTriggerTick TrigTick { get; init; } /* 触发间隔(帧) */
    public required int TrigOdds { get; init; } /* 触发几率 */
    public required List<int> ActionID { get; init; } /* 触发行为 */
    public required bool DeathRemove { get; init; } /* 死亡移除 */
    
    private static OrderedDictionary<int, DataAi> _all = [];

    public static DataAi? Get(int iD)
    {
        return _all.GetValueOrDefault(iD);
    }

    public static IReadOnlyList<DataAi> All()
    {
        return _all.Values;
    }
}
