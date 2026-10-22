namespace Config.Combat;

public partial class DPhaseVariant
{
    public required Combat.DPhaseKey Key { get; init; }
    public required Combat.DEffect Effect { get; init; }
}
