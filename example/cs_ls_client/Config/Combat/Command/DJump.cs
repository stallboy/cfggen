namespace Config.Combat.Command
{

public partial class DJump : Combat.DCommand
{
    public string Key { get; init; } = null!;
    public Combat.DNestedPhase? NullableRefKey { get; internal set; }
}
}
