namespace Config.Other;

public partial class DataDropItem
{
    public required int Chance { get; init; } /* 掉落概率 */
    public required List<int> Itemids { get; init; } /* 掉落物品 */
    public required int Countmin { get; init; } /* 数量下限 */
    public required int Countmax { get; init; } /* 数量上限 */
}
