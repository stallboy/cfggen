namespace Config.Combat.Command;

public partial class DJump : Combat.DCommand
{
    public required string Key { get; init; }
    public Combat.DNestedPhase? NullableRefKey { get; internal set; }
}
