namespace Config.Combat;

public partial class DFrame
{
    public required long Tick { get; init; }
    public required Combat.DEffect Effect { get; init; }
}
