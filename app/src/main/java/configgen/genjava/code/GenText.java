package configgen.genjava.code;

import configgen.i18n.LangSwitchable;
import configgen.util.CachedIndentPrinter;

import java.util.List;
import java.util.stream.Collectors;

import static configgen.gen.Generator.lower1;
import static configgen.gen.Generator.upper1;

class GenText {

    static void generate(LangSwitchable ls, CachedIndentPrinter ps) {
        ps.println("package %s;", Name.codeTopPkg);
        ps.println();

        ps.println("public class Text {");

        List<String> languages = ls.languages();
        //fields
        for (String lang : languages) {
            ps.println1("private String %s;", lower1(lang));
        }

        //constructor
        ps.println1("private Text() {");
        ps.println1("}");
        ps.println();

        ps.println1("public Text(%s) {", languages.stream().map(e -> "String " + lower1(e)).collect(Collectors.joining(", ")));
        for (String lang : languages) {
            String langStr = lower1(lang);
            ps.println2("this.%s = %s;", langStr, langStr);
        }
        ps.println1("}");
        ps.println();

        ps.println1("public static Text _create(configgen.genjava.ConfigInput input) {");
        ps.println2("Text self = new Text();");
        for (String lang : languages) {
            ps.println2("self.%s = input.readStr();", lower1(lang));
        }
        ps.println2("return self;");
        ps.println1("}");
        ps.println();


        //getters
        for (String lang : languages) {
            ps.println1("public String get%s() {", upper1(lang));
            ps.println2("return %s;", lower1(lang));
            ps.println1("}");
            ps.println();
        }

        ps.println("}");
    }
}
