using System;
using System.Collections.Generic;
namespace Config.Other
{

public partial class DDropItem
{
    public int Chance { get; init; } /* 掉落概率 */
    public List<int> Itemids { get; init; } = null!; /* 掉落物品 */
    public int Countmin { get; init; } /* 数量下限 */
    public int Countmax { get; init; } /* 数量上限 */
}
}
