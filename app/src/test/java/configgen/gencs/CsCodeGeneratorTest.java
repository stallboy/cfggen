package configgen.gencs;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.CliException;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C# 生成器冒烟：生成成功 + 关键产物存在 + 内容含预期类型。
 */
class CsCodeGeneratorTest {

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
        Path outDir = tempDir.resolve("csout");
        new CsCodeGenerator(new ParameterParser("cs,dir:" + outDir)).generate(ctx);

        Path configDir = outDir.resolve("Config");
        Path userCs = configDir.resolve("DUser.cs");
        assertTrue(Files.exists(userCs), "应生成Config/DUser.cs（默认前缀D、pkg默认Config）");
        String content = Files.readString(userCs);
        assertTrue(content.contains("DUser"), "应包含类型名DUser");
        assertTrue(content.contains("Name"), "应包含字段Name");

        assertTrue(Files.exists(configDir.resolve("Loader.cs")), "应生成Config/Loader.cs");
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
        Path outDir = tempDir.resolve("csout_enum");
        new CsCodeGenerator(new ParameterParser("cs,dir:" + outDir)).generate(ctx);

        Path abilityCs = outDir.resolve("Config/DAbility.cs");
        assertTrue(Files.exists(abilityCs), "应生成Config/DAbility.cs");
        String content = Files.readString(abilityCs);
        assertTrue(content.contains("Fireball"), "枚举值应写入生成的枚举类型");
        assertTrue(content.contains("IceSpike"), "枚举值应写入生成的枚举类型");
    }

    @Test
    void constructor_invalidPrefix_throws() {
        // 非法前缀应 fail-fast；C# 标识符不允许 $ 和连字符、数字不能开头
        assertThrows(CliException.class, () -> new CsCodeGenerator(new ParameterParser("cs,prefix:1a")));
        assertThrows(CliException.class, () -> new CsCodeGenerator(new ParameterParser("cs,prefix:my-c")));
        assertThrows(CliException.class, () -> new CsCodeGenerator(new ParameterParser("cs,prefix:$x")));
        assertDoesNotThrow(() -> new CsCodeGenerator(new ParameterParser("cs,prefix:D")));
        assertDoesNotThrow(() -> new CsCodeGenerator(new ParameterParser("cs,prefix:_Cfg")));
    }
}
