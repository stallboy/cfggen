package configgen.geni18n;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.Parameter;
import configgen.gen.ParameterParser;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * i18n（按值翻译）生成器冒烟：收集所有 text 字段输出为 csv（表名,原文,译文）。
 */
class I18nByValueGeneratorTest {

    private @TempDir Path tempDir;

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger() {
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    @Test
    void generate_collectTextFieldsToCsv() throws IOException {
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                    desc:text;
                }
                """;
        String csv = """
                用户ID,名字,描述
                id,name,desc
                1,Alice,你好世界
                """;

        Context ctx = TestCtx.newContext(tempDir, cfgStr, Map.of("user", csv));
        Path outFile = tempDir.resolve("i18nout/en.csv");
        Files.createDirectories(outFile.getParent());
        new I18nByValueGenerator(new ParameterParser("i18n,file:" + outFile)).generate(ctx);

        assertTrue(Files.exists(outFile), "应生成翻译csv");
        String content = Files.readString(outFile);
        assertTrue(content.contains("user"), "应包含表名");
        assertTrue(content.contains("你好世界"), "应包含text字段原文（译文列为空，待翻译）");
        assertFalse(content.contains("Alice"), "str字段不应被收集");
    }
}
