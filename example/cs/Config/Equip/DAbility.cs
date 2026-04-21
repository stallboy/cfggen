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
    
    private static System.Collections.Frozen.FrozenDictionary<int, DAbilityInfo> _all = null!;

    public static DAbilityInfo? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DAbilityInfo> All()
    {
        return _all.Values;
    }
}

public static class DAbilityExtensions
{
    internal static readonly DAbilityInfo[] _infos = new DAbilityInfo[7];

    public static DAbilityInfo Info(this DAbility e)
    {
        return _infos[(int)e];
    }
}
