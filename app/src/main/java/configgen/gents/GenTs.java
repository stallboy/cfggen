package configgen.gents;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.i18n.LangSwitch;
import configgen.schema.*;
import configgen.util.CachedIndentPrinter;
import configgen.util.JteEngine;
import configgen.value.CfgValue;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;

public class GenTs extends GeneratorWithTag {
    public final String pkg;
    public final String encoding;
    private final Path dstFile;
    public CfgValue cfgValue;
    public CfgSchema cfgSchema;
    public LangSwitch nullableLanguageSwitch;

    public GenTs(Parameter parameter) {
        super(parameter);
        dstFile = Path.of(parameter.get("file", "Config.ts"));
        pkg = parameter.get("pkg", "Config");
        encoding = parameter.get("encoding", "UTF-8");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        cfgValue = ctx.makeValue(tag);  // 这里只需要schema，生成value只用于检验数据
        cfgSchema = cfgValue.schema();
        nullableLanguageSwitch = ctx.nullableLangSwitch();

        try (CachedIndentPrinter ps = new CachedIndentPrinter(dstFile, encoding)) {
            JteEngine.render("ts/Config.jte", this, ps);
        }
    }

    public String className(Nameable nameable) {
        String[] seps = nameable.fullName().split("\\.");
        return String.join("_", Arrays.stream(seps).map(Generator::upper1).toList());
    }
}
