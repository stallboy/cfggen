@import java.util.List
@param String pkg
@param List<String> languages
@param boolean unity

${unity ? "namespace " + pkg + "\n{" : "namespace " + pkg + ";"}

public partial class Text
{
    @for(String lang : languages)
        @if(unity)
        public string ${lang} { get; init; } = null!;
        @else
        public required string ${lang} { get; init; }
        @endif
    @endfor
    private Text() {}

    public override string ToString()
    {
        return "(" + ${String.join(" + \",\" + ", languages)} + ")";
    }

    internal static Text _create(ConfigReader reader)
    {
        int index = reader.ReadTextIndex();
        var pool = reader.CaptureTextPool();
        return new Text
        {
        @for(int i = 0; i < languages.size(); i++)
            ${languages.get(i)} = pool.Texts[${i}][index],
        @endfor
        };
    }
}
${unity ? "}" : ""}
