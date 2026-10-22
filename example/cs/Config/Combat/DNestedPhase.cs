namespace Config.Combat;

public partial class DNestedPhase
{
    public required string Key { get; init; }
    public required Combat.DCommand Action { get; init; }
}
