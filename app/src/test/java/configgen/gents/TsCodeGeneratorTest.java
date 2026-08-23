package configgen.gents;

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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TypeScript 生成器冒烟：生成成功 + 关键产物存在 + 内容含预期类型。
 */
class TsCodeGeneratorTest {

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
    void generate_simpleTable() throws IOException {
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;
        String csv = """
                用户ID,名字
                id,name
                1,Alice
                """;

        Context ctx = TestCtx.newContext(tempDir, cfgStr, Map.of("user", csv));
        Path outDir = tempDir.resolve("tsout");
        new TsCodeGenerator(new ParameterParser("ts,dir:" + outDir)).generate(ctx);

        Path configTs = outDir.resolve("Config.ts");
        assertTrue(Files.exists(configTs), "应生成Config.ts");
        String content = Files.readString(configTs);
        assertTrue(content.contains("user"), "Config.ts应包含表user的类型定义");

        assertTrue(Files.exists(outDir.resolve("ConfigUtil.ts")), "应生成ConfigUtil.ts");
    }

    @Test
    void generate_enumTable() throws IOException {
        String cfgStr = """
                table ability[id] (enum='name') {
                    id:int;
                    name:str;
                }
                """;
        String csv = """
                技能ID,技能名称
                id,name
                1,Fireball
                2,IceSpike
                """;

        Context ctx = TestCtx.newContext(tempDir, cfgStr, Map.of("ability", csv));
        Path outDir = tempDir.resolve("tsout_enum");
        new TsCodeGenerator(new ParameterParser("ts,dir:" + outDir)).generate(ctx);

        String content = Files.readString(outDir.resolve("Config.ts"));
        assertTrue(content.contains("Fireball"), "枚举值应写入生成的类型");
        assertTrue(content.contains("IceSpike"), "枚举值应写入生成的类型");
    }
}
