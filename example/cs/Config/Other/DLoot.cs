namespace Config.Other;

public partial class DLoot
{
    public required int Lootid { get; init; } /* 序号 */
    public required string Ename { get; init; }
    public required string Name { get; init; } /* 名字 */
    public required List<int> ChanceList { get; init; } /* 掉落0件物品的概率 */
    public List<Other.DLootitem> ListRefLootid { get; private set; } = null!;
    public List<Other.DLootitem> ListRefAnotherWay { get; private set; } = null!;
    
    private static OrderedDictionary<int, DLoot> _all = [];

    public static DLoot? Get(int lootid)
    {
        return _all.GetValueOrDefault(lootid);
    }

    public static IReadOnlyList<DLoot> All()
    {
        return _all.Values;
    }
}
