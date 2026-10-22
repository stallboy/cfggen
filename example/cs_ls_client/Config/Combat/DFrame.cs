namespace Config.Combat
{

public partial class DFrame
{
    public long Tick { get; init; }
    public Combat.DEffect Effect { get; init; } = null!;
}
}
