namespace Config.Equip;

public enum DJewelrytype
{
    Jade,
    Bracelet,
    Magic,
    Bottle,
}

public partial class DJewelrytypeInfo
{
    public required string TypeName { get; init; } /* 程序用名字 */
    public required DJewelrytype eEnum { get; init; }
    
    private static System.Collections.Frozen.FrozenDictionary<string, DJewelrytypeInfo> _all = null!;

    public static DJewelrytypeInfo? Get(string typeName)
    {
        return _all.GetValueOrDefault(typeName);
    }

    public static IReadOnlyList<DJewelrytypeInfo> All()
    {
        return _all.Values;
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
