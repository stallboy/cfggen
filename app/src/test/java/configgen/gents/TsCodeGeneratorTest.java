package configgen.gents;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import configgen.genbyai.TsSchemaGenerator;
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
 * TypeScript 生成器的产物检查。
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
    void exportsRootDefinitionsWithoutAnAnonymousNamespace() throws IOException {
        Context context = TestCtx.newContext(tempDir, """
                table user[id] { id:int; name:str; }
                """, Map.of("user", "id,name\nid,name\n1,Alice\n"));
        Path outDir = tempDir.resolve("schemaout");
        new TsSchemaGenerator(new ParameterParser("tsschema,table:user,dst:" + outDir)).generate(context);

        assertTrue(Files.readString(outDir.resolve("user.ts")).startsWith("export interface user {"));
    }

    @Test
    void generatesConfigAndRuntimeSources() throws IOException {
        String cfgStr = """
                struct EmptyValue {}
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
        assertTrue(Files.readString(configTs).contains("return \"()\";"), "空结构应输出空括号");
        assertTrue(Files.exists(outDir.resolve("ConfigUtil.ts")), "应生成ConfigUtil.ts");
    }
}
