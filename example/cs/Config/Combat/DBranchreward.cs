namespace Config.Combat;

public partial class DBranchreward
{
    public required Combat.DPhaseKey Key { get; init; }
    public required Combat.DReward Reward { get; init; }

    public static DBranchreward? Get(Combat.DPhaseKey key)
        => Get(ConfigMgr.GetMgr(), key);

    public static DBranchreward? Get(ConfigMgr config, Combat.DPhaseKey key)
    {
        var index = config._combat_dbranchreward_All;
        return index.TryGetValue(key, out var result) ? result : null;
    }

    public static IReadOnlyList<DBranchreward> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DBranchreward> All(ConfigMgr config)
        => config._combat_dbranchreward_Rows;
}
