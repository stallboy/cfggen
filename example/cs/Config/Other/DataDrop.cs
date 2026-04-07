namespace Config.Other;

public partial class DataDrop
{
    public required int Dropid { get; init; } /* 序号 */
    public required string Name { get; init; } /* 名字 */
    public required List<Other.DataDropItem> Items { get; init; } /* 掉落概率 */
    public required OrderedDictionary<int, int> Testmap { get; init; } /* 测试map block */
    
    private static OrderedDictionary<int, DataDrop> _all = [];

    public static DataDrop? Get(int dropid)
    {
        return _all.GetValueOrDefault(dropid);
    }

    public static IReadOnlyList<DataDrop> All()
    {
        return _all.Values;
    }
}
