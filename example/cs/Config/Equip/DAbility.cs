namespace Config.Equip;

public enum DAbility
{
    Attack,
    Defence,
    Hp,
    Critical,
    Critical_resist,
    Block,
    Break_armor,
}

public partial class DAbilityInfo
{
    public required int Id { get; init; } /* 属性类型 */
    public required string Name { get; init; } /* 程序用名字 */
    public required DAbility EEnum { get; init; }

    public static DAbilityInfo? Get(int id)
        => Get(ConfigMgr.GetMgr(), id);

    public static DAbilityInfo? Get(ConfigMgr config, int id)
    {
        var index = config._equip_dability_All;
        return index.TryGetValue(id, out var result) ? result : null;
    }

    public static IReadOnlyList<DAbilityInfo> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DAbilityInfo> All(ConfigMgr config)
        => config._equip_dability_Rows;
}

public static class DAbilityExtensions
{
    public static DAbilityInfo Info(this DAbility e)
        => e.Info(ConfigMgr.GetMgr());

    public static DAbilityInfo Info(this DAbility e, ConfigMgr config)
        => config._equip_dability_Symbols[(int)e]!;
}
