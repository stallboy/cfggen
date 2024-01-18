package configgen.gencs;

import configgen.gen.Generator;
import configgen.gen.LangSwitch;
import configgen.util.CachedIndentPrinter;

import java.util.List;

public class GenText {
    public static void generate(LangSwitch langSwitch, String pkg, CachedIndentPrinter ps) {
        if (!pkg.equals("Config")) {
            ps.println("using Config;");
        }

        ps.println("namespace " + pkg);
        ps.println("{");

        ps.println1("public partial class Text");
        ps.println1("{");

        List<String> languages = langSwitch.languages().stream().map(Generator::upper1).toList();
        //fields
        for (String lang : languages) {
            ps.println2("public string %s { get; private set; }", lang);
        }

        ps.println2("private Text() {}");
        ps.println();

        ps.println2("public override string ToString()");
        ps.println2("{");
        ps.println3("return \"(\" + " + String.join(" + \",\" + ", languages) + " + \")\";");
        ps.println2("}");
        ps.println();

        ps.println2("internal static Text _create(Config.Stream os)");
        ps.println2("{");
        ps.println3("Text self = new Text();");
        for (String lang : languages) {
            ps.println3("self.%s = os.ReadString();", lang);
        }
        ps.println3("return self;");
        ps.println2("}");
        ps.println1("}");
        ps.println("}");
    }
}
