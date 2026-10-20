namespace Config.Other;

public partial class DLoot
{
    public required int Lootid { get; init; } /* 序号 */
    public required string Ename { get; init; }
    public required string Name { get; init; } /* 名字 */
    public required IReadOnlyList<int> ChanceList { get; init; } /* 掉落0件物品的概率 */
    public IReadOnlyList<Other.DLootitem> ListRefLootid { get; private set; } = null!;
    public IReadOnlyList<Other.DLootitem> ListRefAnotherWay { get; private set; } = null!;

    public static DLoot? Get(int lootid)
        => Get(ConfigMgr.GetMgr(), lootid);

    public static DLoot? Get(ConfigMgr config, int lootid)
    {
        var index = config._other_dloot_All;
        return index.TryGetValue(lootid, out var result) ? result : null;
    }

    public static IReadOnlyList<DLoot> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DLoot> All(ConfigMgr config)
        => config._other_dloot_Rows;
}
