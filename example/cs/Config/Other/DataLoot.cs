namespace Config.Other;

public partial class DataLoot
{
    public required int Lootid { get; init; } /* 序号 */
    public required string Ename { get; init; }
    public required string Name { get; init; } /* 名字 */
    public required List<int> ChanceList { get; init; } /* 掉落0件物品的概率 */
    public List<Other.DataLootitem> ListRefLootid { get; private set; } = null!;
    public List<Other.DataLootitem> ListRefAnotherWay { get; private set; } = null!;
    
    private static OrderedDictionary<int, DataLoot> _all = [];

    public static DataLoot? Get(int lootid)
    {
        return _all.GetValueOrDefault(lootid);
    }

    public static IReadOnlyList<DataLoot> All()
    {
        return _all.Values;
    }
}
