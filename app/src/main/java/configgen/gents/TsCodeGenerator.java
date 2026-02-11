package configgen.gents;

import configgen.ctx.Context;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.i18n.LangSwitchable;
import configgen.schema.*;
import configgen.util.CachedIndentPrinter;
import configgen.util.FileUtil;
import configgen.util.JteEngine;
import configgen.util.StringUtil;
import configgen.value.CfgValue;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;

public class TsCodeGenerator extends GeneratorWithTag {
    public final String pkg;
    public final String encoding;
    public final boolean serverText;
    private final Path dstDir;
    public CfgValue cfgValue;
    public CfgSchema cfgSchema;
    public LangSwitchable nullableLanguageSwitch;

    public TsCodeGenerator(Parameter parameter) {
        super(parameter);
        dstDir = Path.of(parameter.get("dir", "."));
        pkg = parameter.get("pkg", "Config");
        encoding = parameter.get("encoding", "UTF-8");
        serverText = parameter.has("serverText");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        cfgValue = ctx.makeValue(tag);  // 这里只需要schema，生成value只用于检验数据
        cfgSchema = cfgValue.schema();
        nullableLanguageSwitch = ctx.nullableLangSwitch();

        try (var ps = new CachedIndentPrinter(dstDir.resolve("Config.ts"), encoding)) {
            JteEngine.render("ts/Config.jte", this, ps);
        }

        FileUtil.copyFileIfNotExist("/support/ts/ConfigUtil.ts",
                "src/main/resources/support/ts/ConfigUtil.ts",
                dstDir.resolve("ConfigUtil.ts"),
                encoding);

    }

    public String className(Nameable nameable) {
        String[] s = nameable.fullName().split("\\.");
        return String.join("_", Arrays.stream(s).map(StringUtil::upper1).toList());
    }

}
