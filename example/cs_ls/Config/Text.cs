namespace Config;

public partial class Text
{
    public required string Zh_cn { get; init; }
    public required string En { get; init; }
    public required string Tw { get; init; }
    private Text() {}

    public override string ToString()
    {
        return "(" + Zh_cn + "," + En + "," + Tw + ")";
    }

    internal static Text _create(Config.Stream os)
    {
        string[] texts = os.ReadTextsInPool();
        return new Text
        {
        Zh_cn = texts[0],
        En = texts[1],
        Tw = texts[2],
        };
    }
}

