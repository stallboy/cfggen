@import configgen.gen.Generator
@import configgen.genjava.code.TextModel
@param TextModel model

package ${model.pkg};

public class Text {
    @for(String lang : model.languages)
    private String ${lang};
    @endfor

    private Text() {
    }

    public Text(@for(int i = 0; i < model.languages.size(); i++)@if(i > 0), @endifString ${model.languages.get(i)}@endfor) {
        @for(String lang : model.languages)
        this.${lang} = ${lang};
        @endfor
    }

    public static Text _create(configgen.genjava.ConfigInput input) {
        Text self = new Text();
        @for(String lang : model.languages)
        self.${lang} = input.readStr();
        @endfor
        return self;
    }

    @for(String lang : model.languages)
    public String get${Generator.upper1(lang)}() {
        return ${lang};
    }

    @endfor
}