namespace Config;

public partial class DataLevelRank
{
    public required int Level { get; init; } /* 等级 */
    public required int Rank { get; init; } /* 品质 */
    public Equip.DataRank RefRank { get; private set; } = null!;
}
