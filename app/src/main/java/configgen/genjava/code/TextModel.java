package configgen.genjava.code;

import configgen.gen.Generator;
import configgen.util.StringUtil;

import java.util.List;

public class TextModel {
    public final String pkg;
    public final List<String> languages;

    public TextModel(String pkg, List<String> languages) {
        this.pkg = pkg;
        this.languages = languages.stream().map(StringUtil::lower1).toList();
    }

}
