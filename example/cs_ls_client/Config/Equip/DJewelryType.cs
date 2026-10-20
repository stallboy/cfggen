using System.Collections.Generic;
namespace Config.Equip
{

public enum DJewelryType
{
    Jade,
    Bracelet,
    Magic,
    Bottle,
}

public partial class DJewelryTypeInfo
{
    public string TypeName { get; init; } = null!; /* 程序用名字 */
    public DJewelryType EEnum { get; init; }

    public static DJewelryTypeInfo? Get(string typeName)
        => Get(ConfigMgr.GetMgr(), typeName);

    public static DJewelryTypeInfo? Get(ConfigMgr config, string typeName)
    {
        var index = config._equip_djewelry_utype_All;
        return index.TryGetValue(typeName, out var result) ? result : null;
    }

    public static IReadOnlyList<DJewelryTypeInfo> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DJewelryTypeInfo> All(ConfigMgr config)
        => config._equip_djewelry_utype_Rows;
}

public static class DJewelryTypeExtensions
{
    public static DJewelryTypeInfo Info(this DJewelryType e)
        => e.Info(ConfigMgr.GetMgr());

    public static DJewelryTypeInfo Info(this DJewelryType e, ConfigMgr config)
        => config._equip_djewelry_utype_Symbols[(int)e]!;
}
}
