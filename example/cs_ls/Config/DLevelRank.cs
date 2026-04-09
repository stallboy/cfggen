namespace Config;

public partial class DLevelRank
{
    public required int Level { get; init; } /* 等级 */
    public required int Rank { get; init; } /* 品质 */
    public Equip.DRank RefRank { get; private set; }
}
