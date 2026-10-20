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

    public static DJewelrytypeInfo? Get(string typeName)
        => Get(ConfigMgr.GetMgr(), typeName);

    public static DJewelrytypeInfo? Get(ConfigMgr config, string typeName)
    {
        var index = config._equip_djewelrytype_All;
        return index.TryGetValue(typeName, out var result) ? result : null;
    }

    public static IReadOnlyList<DJewelrytypeInfo> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DJewelrytypeInfo> All(ConfigMgr config)
        => config._equip_djewelrytype_Rows;
}

public static class DJewelrytypeExtensions
{
    public static DJewelrytypeInfo Info(this DJewelrytype e)
        => e.Info(ConfigMgr.GetMgr());

    public static DJewelrytypeInfo Info(this DJewelrytype e, ConfigMgr config)
        => config._equip_djewelrytype_Symbols[(int)e]!;
}
}
