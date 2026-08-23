package configgen.gents;

import configgen.naming.GenNaming;
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
        // 注意：ts 不做 deleteOtherFiles/keepMetaAndDeleteOtherFiles 收尾清理，这是目录语义决定的、不是遗漏：
        // 1. dstDir 默认就是用户项目根（不传 dir 时为 "."），Config.ts 与 main.ts/package.json 等
        //    用户文件同目录，任何清理都会误删它们；cs/lua 的清理作用在 dir.resolve(pkg) 的
        //    生成器独占子目录，gd 依赖调用方显式传独占目录（dir:config），ts 没有独占目录。
        // 2. ts 产物仅 Config.ts + ConfigUtil.ts 两个固定文件名，不会产生陈旧文件堆积，无清理需求。
        cfgValue = ctx.makeValue(tag);  // 这里只需要schema，生成value只用于检验数据
        cfgSchema = cfgValue.schema();
        nullableLanguageSwitch = ctx.nullableLangSwitch();

        try (var ps = new CachedIndentPrinter(dstDir.resolve("Config.ts"), encoding, ctx.outputFiles())) {
            JteEngine.render("ts/Config.jte", this, ps);
        }

        FileUtil.copyFileIfNotExist("/support/ts/ConfigUtil.ts",
                "src/main/resources/support/ts/ConfigUtil.ts",
                dstDir.resolve("ConfigUtil.ts"),
                encoding, ctx.outputFiles());

    }

    public String className(Nameable nameable) {
        return String.join("_", GenNaming.classNameSegments(nameable).stream().map(StringUtil::upper1).toList());
    }

}
