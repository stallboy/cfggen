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
    texts := stream.ReadTextsInPool()
    @for(int i = 0; i < languages.size(); i++)
    if ${i} < len(texts) {
        self.${languages.get(i)} = texts[${i}]
    }
    @endfor
    return self
}

func (t *Text) String() string {
    @if(languages.size() == 1)
    return t.${languages.getFirst()}
    @else
    result := ""
    @for(int i = 0; i < languages.size(); i++)
    @if(i > 0)
    result += ","
    @endif
    result += t.${languages.get(i)}
    @endfor
    return result
    @endif
}
