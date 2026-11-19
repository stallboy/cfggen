@import configgen.genjava.code.TextModel
@import configgen.util.StringUtil
@param TextModel model

package ${model.pkg};

public class Text {
    @for(String lang : model.languages)
    private final String ${lang};
    @endfor

${"\n"}    public Text(@for(int i = 0; i < model.languages.size(); i++)@if(i > 0), @endifString ${model.languages.get(i)}@endfor) {
        @for(String lang : model.languages)
        this.${lang} = ${lang};
        @endfor
    }

    private Text(configgen.genjava.ConfigInput input) {
        int index = input.readTextIndex();
        @for(int i = 0; i < model.languages.size(); i++)
        this.${model.languages.get(i)} = input.textInPool(${i}, index);
        @endfor
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

    public static Text _create(configgen.genjava.ConfigInput input) {
        return new Text(input);
    }
}
