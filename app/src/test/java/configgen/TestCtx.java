package configgen;

import configgen.ctx.Context;
import configgen.data.HeadRows;

import java.nio.file.Path;
import java.util.Map;

/**
 * 生成器测试的公共脚手架：把 schema 与若干 csv 写入临时目录后构造 Context。
 */
public final class TestCtx {
    private TestCtx() {
    }

    public static Context newContext(Path tempDir, String cfgStr, Map<String, String> csvByName) {
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        csvByName.forEach((name, csv) -> Resources.addTempFileFromText(name + ".csv", tempDir, csv));
        return new Context(tempDir);
    }

    /**
     * 带多语言切换的 Context：langsDir 下放若干 <lang>.csv（byValue模式），defaultLang 为原文语言。
     */
    public static Context newLangSwitchContext(Path tempDir, String cfgStr, Map<String, String> csvByName,
                                               Path langsDir, String defaultLang) {
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        csvByName.forEach((name, csv) -> Resources.addTempFileFromText(name + ".csv", tempDir, csv));
        return new Context(new Context.ContextCfg(tempDir, null, HeadRows.A2_Default, "UTF-8",
                null, langsDir.toString(), defaultLang, false));
    }
}
