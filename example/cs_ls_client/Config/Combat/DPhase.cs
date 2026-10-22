namespace Config.Combat
{

public partial class DPhase
{
    public string Key { get; init; } = null!;
    public string Next { get; init; } = null!;
    public Combat.DPhase? NullableRefNext { get; internal set; }
    public Combat.DEffect RefExit { get; internal set; } = null!;
}
}
