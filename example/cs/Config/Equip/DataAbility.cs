namespace Config.Equip;

public enum DataAbility
{
    Attack,
    Defence,
    Hp,
    Critical,
    Critical_resist,
    Block,
    Break_armor,
}

public partial class DataAbilityInfo
{
    public required int Id { get; init; } /* 属性类型 */
    public required string Name { get; init; } /* 程序用名字 */
    public required DataAbility eEnum { get; init; }
    
    private static OrderedDictionary<int, DataAbilityInfo> _all = [];

    public static DataAbilityInfo? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DataAbilityInfo> All()
    {
        return _all.Values;
    }
}

public static class DataAbilityExtensions
{
    internal static readonly DataAbilityInfo[] _infos = new DataAbilityInfo[7];

    public static DataAbilityInfo Info(this DataAbility e)
    {
        return _infos[(int)e];
    }
}
