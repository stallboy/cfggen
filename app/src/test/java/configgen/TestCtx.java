package configgen;

import configgen.ctx.Context;

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
}
