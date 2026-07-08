@import java.util.List
@param String pkg
@param List<String> languages
@param boolean unity
@if(unity)
using System;
using System.Collections.Generic;
@endif

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
        string[] texts = reader.ReadTextsInPool();
        return new Text
        {
        @for(int i = 0; i < languages.size(); i++)
            ${languages.get(i)} = texts[${i}],
        @endfor
        };
    }
}
${unity ? "}" : ""}
