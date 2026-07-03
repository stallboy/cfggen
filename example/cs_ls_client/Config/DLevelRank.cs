using System;
using System.Collections.Generic;
namespace Config
{

public partial class DLevelRank
{
    public int Level { get; init; } /* 等级 */
    public int Rank { get; init; } /* 品质 */
    public Equip.DRank RefRank { get; private set; }
}
}
