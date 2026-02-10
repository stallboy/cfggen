@import configgen.genjava.code.TextModel
@import configgen.util.StringUtil
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
        String[] texts = input.readTextsInPool();
        @for(int i = 0; i < model.languages.size(); i++)
        self.${model.languages.get(i)} = texts[${i}];
        @endfor
        return self;
    }

    @for(String lang : model.languages)
    public String get${StringUtil.upper1(lang)}() {
        return ${lang};
    }

    @endfor
    @Override
    public String toString() {
        return "Text(" + ${model.join()} + ")";
    }
}