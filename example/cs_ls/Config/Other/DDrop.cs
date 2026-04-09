namespace Config.Other;

public partial class DDrop
{
    public required int Dropid { get; init; } /* 序号 */
    public required Config.Text Name { get; init; } /* 名字 */
    public required List<Other.DDropItem> Items { get; init; } /* 掉落概率 */
    public required OrderedDictionary<int, int> Testmap { get; init; } /* 测试map block */
    
    private static System.Collections.Frozen.FrozenDictionary<int, DDrop> _all = null!;

    public static DDrop? Get(int dropid)
    {
        return _all.GetValueOrDefault(dropid);
    }

    public static IReadOnlyList<DDrop> All()
    {
        return _all.Values;
    }
}
