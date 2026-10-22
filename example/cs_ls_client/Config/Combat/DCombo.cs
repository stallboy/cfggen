namespace Config.Combat
{

public partial class DCombo
{
    public string Weapon { get; init; } = null!;
    public int Tier { get; init; }
    public bool Airborne { get; init; }
    public Combat.DEffect Effect { get; init; } = null!;
}
}
