using System;
using System.Collections.Generic;
namespace Config.Equip
{

public enum DJewelrytype
{
    Jade,
    Bracelet,
    Magic,
    Bottle,
}

public partial class DJewelrytypeInfo
{
    public string TypeName { get; init; } = null!; /* 程序用名字 */
    public DJewelrytype EEnum { get; init; }
    private static IReadOnlyList<DJewelrytypeInfo> _allList = null!;
    
    private static Dictionary<string, DJewelrytypeInfo> _all = null!;

    public static DJewelrytypeInfo? Get(string typeName)
    {
        return _all.GetValueOrDefault(typeName);
    }

    public static IReadOnlyList<DJewelrytypeInfo> All()
    {
        return _allList;
    }
}

public static class DJewelrytypeExtensions
{
    internal static readonly DJewelrytypeInfo[] _infos = new DJewelrytypeInfo[4];

    public static DJewelrytypeInfo Info(this DJewelrytype e)
    {
        return _infos[(int)e];
    }
}
}
