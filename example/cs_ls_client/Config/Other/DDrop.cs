using System.Collections.Generic;
namespace Config.Other
{

public partial class DDrop
{
    public int Dropid { get; init; } /* 序号 */
    public Config.Text Name { get; init; } = null!; /* 名字 */
    public IReadOnlyList<Other.DDropItem> Items { get; init; } = null!; /* 掉落概率 */
    public IReadOnlyDictionary<int, int> Testmap { get; init; } = null!; /* 测试map block */

    public static DDrop? Get(int dropid)
        => Get(ConfigMgr.GetMgr(), dropid);

    public static DDrop? Get(ConfigMgr config, int dropid)
    {
        var index = config._other_ddrop_All;
        return index.TryGetValue(dropid, out var result) ? result : null;
    }

    public static IReadOnlyList<DDrop> All()
        => All(ConfigMgr.GetMgr());

    public static IReadOnlyList<DDrop> All(ConfigMgr config)
        => config._other_ddrop_Rows;
}
}
