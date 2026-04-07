namespace Config.Equip;

public partial class DataJewelrytype
{
    public static DataJewelrytype Jade { get; private set; } = null!;
    public static DataJewelrytype Bracelet { get; private set; } = null!;
    public static DataJewelrytype Magic { get; private set; } = null!;
    public static DataJewelrytype Bottle { get; private set; } = null!;

    public required string TypeName { get; init; } /* 程序用名字 */
    
    private static OrderedDictionary<string, DataJewelrytype> _all = [];

    public static DataJewelrytype? Get(string typeName)
    {
        return _all.GetValueOrDefault(typeName);
    }

    public static IReadOnlyList<DataJewelrytype> All()
    {
        return _all.Values;
    }
}
