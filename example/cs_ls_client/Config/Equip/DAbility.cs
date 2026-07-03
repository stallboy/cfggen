using System;
using System.Collections.Generic;
namespace Config.Equip
{

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
    public int Id { get; init; } /* 属性类型 */
    public string Name { get; init; } = null!; /* 程序用名字 */
    public DAbility EEnum { get; init; }
    private static IReadOnlyList<DAbilityInfo> _allList = null!;
    
    private static Dictionary<int, DAbilityInfo> _all = null!;

    public static DAbilityInfo? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DAbilityInfo> All()
    {
        return _allList;
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
}
