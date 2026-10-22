namespace Config.Combat;

public partial class DCombo
{
    public required string Weapon { get; init; }
    public required int Tier { get; init; }
    public required bool Airborne { get; init; }
    public required Combat.DEffect Effect { get; init; }
}
