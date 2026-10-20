using System.Collections.Generic;
namespace Config.Other
{

public partial class DLoot
{
    public int Lootid { get; init; } /* 序号 */
    public string Ename { get; init; } = null!;
    public Config.Text Name { get; init; } = null!; /* 名字 */
    public IReadOnlyList<int> ChanceList { get; init; } = null!; /* 掉落0件物品的概率 */
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
}
