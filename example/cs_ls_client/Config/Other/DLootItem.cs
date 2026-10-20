using System.Collections.Generic;
namespace Config.Other
{

public partial class DLootItem
{
    public int Lootid { get; init; } /* 掉落id */
    public int Itemid { get; init; } /* 掉落物品 */
    public int Chance { get; init; } /* 掉落概率 */
    public int Countmin { get; init; } /* 数量下限 */
    public int Countmax { get; init; } /* 数量上限 */

    public sealed record LootidItemidKey(int Lootid, int Itemid);

    public static DLootItem? Get(int lootid, int itemid)
        => Get(ConfigMgr.GetMgr(), lootid, itemid);

    public static DLootItem? Get(ConfigMgr config, int lootid, int itemid)
    {
        var index = config._other_dloot_uitem_All;
        return index.TryGetValue(new LootidItemidKey(lootid, itemid), out var result) ? result : null;
    }

    public static IReadOnlyList<DLootItem> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DLootItem> All(ConfigMgr config)
        => config._other_dloot_uitem_Rows;
}
}
