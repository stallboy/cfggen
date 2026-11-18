package configgen.gencs;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.CliException;
import configgen.gen.ParameterParser;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C# 生成器的文件、参数和字段筛选契约。
 */
class CsCodeGeneratorTest {

    private @TempDir Path tempDir;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void minusEditorPreservesInterfaceReferencesAndExcludesTaggedFields(boolean editorFields) throws IOException {
        Files.createDirectory(tempDir.resolve("td"));
        String editorTag = editorFields ? "(editor)" : "";
        Context ctx = TestCtx.newContext(tempDir, """
                table td.buff[id] { id:int; }
                table td.damagetype[id] { id:int; }
                table td.hero[id] { id:int; }
                interface td.Action {
                    struct ApplyBuff {
                        buff:int ->buff;
                        damageType:int ->damagetype;
                        hero:int ->hero;
                        editorBuff:int ->buff %s;
                        note:str %s;
                    }
                }
                """.formatted(editorTag, editorTag), Map.of(
                "td/buff", "id\nid\n1\n",
                "td/damagetype", "id\nid\n1\n",
                "td/hero", "id\nid\n1\n"));
        Path fullOutput = tempDir.resolve("full");
        new CsCodeGenerator(new ParameterParser("cs,dir:" + fullOutput)).generate(ctx);
        String fullCode = Files.readString(fullOutput.resolve("Config/Td/Action/DApplyBuff.cs"));
        assertTrue(fullCode.contains("int EditorBuff {"));
        assertTrue(fullCode.contains("RefEditorBuff {"));
        assertTrue(fullCode.contains("string Note {"));

        Path filteredOutput = tempDir.resolve("filtered");
        new CsCodeGenerator(new ParameterParser("cs,own:-editor,dir:" + filteredOutput)).generate(ctx);
        String filteredCode = Files.readString(filteredOutput.resolve("Config/Td/Action/DApplyBuff.cs"));

        for (String field : java.util.List.of("Buff", "DamageType", "Hero")) {
            assertTrue(filteredCode.contains("int " + field + " {"), field);
            assertTrue(filteredCode.contains("Ref" + field + " {"), "Ref" + field);
        }
        assertEquals(!editorFields, filteredCode.contains("int EditorBuff {"));
        assertEquals(!editorFields, filteredCode.contains("RefEditorBuff {"));
        assertEquals(!editorFields, filteredCode.contains("string Note {"));
    }

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
        assertTrue(Files.exists(configDir.resolve("Loader.cs")), "应生成Config/Loader.cs");
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
