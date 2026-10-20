
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

    internal static Text _create(ConfigReader reader)
    {
        int index = reader.ReadTextIndex();
        var pool = reader.CaptureTextPool();
        return new Text
        {
        Zh_cn = pool.Texts[0][index],
        En = pool.Texts[1][index],
        Tw = pool.Texts[2][index],
        };
    }
}

