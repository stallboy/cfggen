namespace Config.Equip;

public enum DataJewelrytype
{
    Jade,
    Bracelet,
    Magic,
    Bottle,
}

public partial class DataJewelrytypeInfo
{
    public required string TypeName { get; init; } /* 程序用名字 */
    public required DataJewelrytype eEnum { get; init; }
    
    private static OrderedDictionary<string, DataJewelrytypeInfo> _all = [];

    public static DataJewelrytypeInfo? Get(string typeName)
    {
        return _all.GetValueOrDefault(typeName);
    }

    public static IReadOnlyList<DataJewelrytypeInfo> All()
    {
        return _all.Values;
    }
}

public static class DataJewelrytypeExtensions
{
    internal static readonly DataJewelrytypeInfo[] _infos = new DataJewelrytypeInfo[4];

    public static DataJewelrytypeInfo Info(this DataJewelrytype e)
    {
        return _infos[(int)e];
    }
}
