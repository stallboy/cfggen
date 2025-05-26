package configgen.gencs;

import configgen.gen.Generator;
import configgen.i18n.LangSwitch;
import configgen.util.CachedIndentPrinter;
import configgen.util.JteEngine;

import java.io.File;
import java.util.List;

public class GenText {
    public record TextModel(String pkg,
                            List<String> languages) {
    }

    static void generate(GenCs gen, LangSwitch langSwitch) {
        try (CachedIndentPrinter ps = new CachedIndentPrinter(gen.dstDir.resolve("Text.cs"), gen.encoding)) {
            List<String> languages = langSwitch.languages().stream().map(Generator::upper1).toList();
            JteEngine.render("cs/Text.jte", new TextModel(gen.pkg, languages), ps);
        }
    }
}
