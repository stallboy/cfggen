namespace Config.Combat;

public partial class DPhase
{
    public required string Key { get; init; }
    public required string Next { get; init; }
    public Combat.DPhase? NullableRefNext { get; internal set; }
    public Combat.DEffect RefExit { get; internal set; } = null!;
}
