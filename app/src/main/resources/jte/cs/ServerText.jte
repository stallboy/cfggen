@import java.util.List
@param String pkg
@param List<String> languages
@if(!pkg.equals("Config"))
    using Config;
@endif

namespace ${pkg};

public partial class Text
{
    @for(String lang : languages)
        public required string ${lang} { get; init; }
    @endfor
    private Text() {}

    public override string ToString()
    {
        return "(" + ${String.join(" + \",\" + ", languages)} + ")";
    }

    internal static Text _create(Config.Stream os)
    {
        string[] texts = os.ReadTextsInPool();
        return new Text
        {
        @for(int i = 0; i < languages.size(); i++)
            ${languages.get(i)} = texts[${i}],
        @endfor
        };
    }
}

