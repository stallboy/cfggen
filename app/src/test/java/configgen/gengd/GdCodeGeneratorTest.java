package configgen.gengd;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.CliException;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GDScript 生成器冒烟：生成成功 + 关键产物存在 + 内容含预期类型。
 */
class GdCodeGeneratorTest {

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
        Path outDir = tempDir.resolve("gdout");
        new GdCodeGenerator(new ParameterParser("gd,dir:" + outDir)).generate(ctx);

        Path userGd = findFileContaining(outDir, "user");
        assertTrue(userGd != null, "应生成user相关的.gd文件");

        String content = Files.readString(userGd);
        assertTrue(content.contains("name"), "应包含字段name");

        assertTrue(Files.exists(outDir.resolve("ConfigLoader.gd")), "应生成ConfigLoader.gd");
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
        Path outDir = tempDir.resolve("gdout_enum");
        new GdCodeGenerator(new ParameterParser("gd,dir:" + outDir)).generate(ctx);

        Path abilityGd = findFileContaining(outDir, "ability");
        assertTrue(abilityGd != null, "应生成ability相关的.gd文件");
        String content = Files.readString(abilityGd);
        assertTrue(content.contains("Fireball") || Files.readString(outDir.resolve("ConfigLoader.gd")).contains("Fireball"),
                "枚举值应出现在生成的代码中");
    }

    private static Path findFileContaining(Path dir, String namePart) throws IOException {
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".gd"))
                    .filter(p -> p.getFileName().toString().toLowerCase().contains(namePart))
                    .findFirst().orElse(null);
        }
    }

    @Test
    void constructor_invalidPrefix_throws() {
        // 非法前缀应 fail-fast；标识符不允许 $ 和连字符、数字不能开头
        assertThrows(CliException.class, () -> new GdCodeGenerator(new ParameterParser("gd,prefix:1a")));
        assertThrows(CliException.class, () -> new GdCodeGenerator(new ParameterParser("gd,prefix:my-c")));
        assertThrows(CliException.class, () -> new GdCodeGenerator(new ParameterParser("gd,prefix:$x")));
        assertDoesNotThrow(() -> new GdCodeGenerator(new ParameterParser("gd,prefix:_Cfg")));
    }

    @Test
    void generate_compositeKey_reportsCleanError() {
        String cfgStr = """
                table pair[a,b] {
                    a:int;
                    b:int;
                }
                """;
        String csv = """
                A,B
                a,b
                1,2
                """;

        Context ctx = TestCtx.newContext(tempDir, cfgStr, Map.of("pair", csv));
        Path outDir = tempDir.resolve("gdout_composite");
        // 复合键应提前报清表名并给出处理办法，而不是渲染中途抛裸异常栈
        CliException e = assertThrows(CliException.class,
                () -> new GdCodeGenerator(new ParameterParser("gd,dir:" + outDir)).generate(ctx));
        assertTrue(e.getMessage().contains("pair"), "错误信息应包含表名，实际: " + e.getMessage());
        assertTrue(e.getMessage().contains("nogd"), "错误信息应给出nogd处理提示，实际: " + e.getMessage());
    }

    @Test
    void generate_compositeKey_filteredByOwnSucceeds() {
        String cfgStr = """
                table pair[a,b] (nogd) {
                    a:int;
                    b:int;
                }
                """;
        String csv = """
                A,B
                a,b
                1,2
                """;

        Context ctx = TestCtx.newContext(tempDir, cfgStr, Map.of("pair", csv));
        Path outDir = tempDir.resolve("gdout_composite_filtered");
        // 标了 (nogd) 并用 own:-nogd 过滤后，复合键表被排除，生成应成功
        assertDoesNotThrow(() ->
                new GdCodeGenerator(new ParameterParser("gd,dir:" + outDir + ",own:-nogd")).generate(ctx));
    }
}
