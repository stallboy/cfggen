using System;
using System.Collections.Generic;
namespace Config.Other
{

public partial class DDrop
{
    public int Dropid { get; init; } /* 序号 */
    public Config.Text Name { get; init; } = null!; /* 名字 */
    public List<Other.DDropItem> Items { get; init; } = null!; /* 掉落概率 */
    public OrderedDictionary<int, int> Testmap { get; init; } = null!; /* 测试map block */
    private static IReadOnlyList<DDrop> _allList = null!;
    
    private static Dictionary<int, DDrop> _all = null!;

    public static DDrop? Get(int dropid)
    {
        return _all.GetValueOrDefault(dropid);
    }

    public static IReadOnlyList<DDrop> All()
    {
        return _allList;
    }
}
}
