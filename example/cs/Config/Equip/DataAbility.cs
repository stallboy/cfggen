namespace Config.Equip;

public partial class DataAbility
{
    public static DataAbility Attack { get; private set; } = null!;
    public static DataAbility Defence { get; private set; } = null!;
    public static DataAbility Hp { get; private set; } = null!;
    public static DataAbility Critical { get; private set; } = null!;
    public static DataAbility Critical_resist { get; private set; } = null!;
    public static DataAbility Block { get; private set; } = null!;
    public static DataAbility Break_armor { get; private set; } = null!;

    public required int Id { get; init; } /* 属性类型 */
    public required string Name { get; init; } /* 程序用名字 */
    
    private static OrderedDictionary<int, DataAbility> _all = [];

    public static DataAbility? Get(int id)
    {
        return _all.GetValueOrDefault(id);
    }

    public static IReadOnlyList<DataAbility> All()
    {
        return _all.Values;
    }
}
