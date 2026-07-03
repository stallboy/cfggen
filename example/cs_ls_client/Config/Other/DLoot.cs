using System;
using System.Collections.Generic;
namespace Config.Other
{

public partial class DLoot
{
    public int Lootid { get; init; } /* 序号 */
    public string Ename { get; init; } = null!;
    public Config.Text Name { get; init; } = null!; /* 名字 */
    public List<int> ChanceList { get; init; } = null!; /* 掉落0件物品的概率 */
    public List<Other.DLootitem> ListRefLootid { get; private set; } = null!;
    public List<Other.DLootitem> ListRefAnotherWay { get; private set; } = null!;
    private static IReadOnlyList<DLoot> _allList = null!;
    
    private static Dictionary<int, DLoot> _all = null!;

    public static DLoot? Get(int lootid)
    {
        return _all.GetValueOrDefault(lootid);
    }

    public static IReadOnlyList<DLoot> All()
    {
        return _allList;
    }
}
}
