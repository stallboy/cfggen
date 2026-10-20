@import java.util.List
@param String pkg
@param List<String> languages
package ${pkg}

type Text struct {
    @for(String lang : languages)
    ${lang} string
    @endfor
}

func createText(stream *Stream) *Text {
    self := &Text{}
    index := stream.ReadTextIndex()
    pools := stream.GetLangTextPools()
    @for(int i = 0; i < languages.size(); i++)
        self.${languages.get(i)} = pools[${i}][index]
    @endfor
    return self
}

func (t *Text) String() string {
    @if(languages.size() == 1)
    return t.${languages.getFirst()}
    @else
        return ${String.join(" + \",\" + ", languages.stream().map(lang -> "t." + lang).toList())}
    @endif
}
