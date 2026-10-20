package configgen.genbytes;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import configgen.genjava.ConfigInput;
import configgen.genjava.ConfigPackage;
import configgen.genjava.SchemaBean;
import configgen.genjava.SchemaDeserializer;
import configgen.genjava.SchemaInterface;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static configgen.genjava.SchemaPrimitive.SStr;
import static configgen.genjava.SchemaPrimitive.SText;
import static org.junit.jupiter.api.Assertions.*;

class BytesGeneratorTest {

    private @TempDir Path tempDir;

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger() {
        Logger.setPrinter(Logger.Printer.outPrinter);
    }

    private static final String CFG = """
            table user[id] {
                id:int;
                name:str;
                desc:text;
                tags:list<str> (sep='|');
            }
            """;

    private static final String CSV = """
            用户ID,名字,描述,标签
            id,name,desc,tags
            1,Alice,你好世界,tag1|tag2
            """;

    @Test
    void defaultOutputIsVersionedBasePackageAndDefaultInspectorReadsIt() throws IOException {
        Path output = tempDir.resolve("default");
        Context context = TestCtx.newContext(tempDir, CFG, java.util.Map.of("user", CSV));
        new BytesGenerator(new ParameterParser("bytes,dir:" + output)).generate(context);
        Path file = output.resolve("config.bytes");
        var pack = ConfigPackage.read(new ConfigInput(file));
        assertEquals("base", pack.name());
        org.junit.jupiter.api.Assertions.assertTrue(pack.blockNames().containsAll(java.util.List.of("schema", "strings", "text/default", ".user")));
        org.junit.jupiter.api.Assertions.assertTrue(new configgen.genjava.BytesInspector(file.toString()).match("user").contains("Alice"));
    }

    @Test
    void roundtrip_withSchema_textReadFromLangTextPool() throws IOException {
        Path outDir = tempDir.resolve("out_schema");
        Context ctx = TestCtx.newContext(tempDir, CFG, java.util.Map.of("user", CSV));
        new BytesGenerator(new ParameterParser("bytes,dir:" + outDir)).generate(ctx);

        var pack = ConfigPackage.read(new ConfigInput(outDir.resolve("config.bytes")));
        try (ConfigInput in = pack.block("schema")) {
            SchemaInterface si = (SchemaInterface) SchemaDeserializer.deserialize(in);
            in.requireEnd();

            SchemaBean user = (SchemaBean) si.implementations.get("user");
            assertEquals(SStr, user.columns.get(1).schema(), "str字段应为SStr");
            // 回归：TEXT曾被标成SStr，按schema读数据会走StringPool读错池
            assertEquals(SText, user.columns.get(2).schema(), "text字段应为SText（读LangTextPool）");

            readAndAssertUserData(pack.block(".user"));
        }
    }

    private static void readAndAssertUserData(ConfigInput in) {
        assertEquals(1, in.readInt(), "记录数");
        assertEquals(1, in.readInt(), "id");
        assertEquals("Alice", in.readStringInPool(), "str字段走StringPool");
        assertEquals("你好世界", in.readTextInPool(), "text字段走LangTextPool");
        assertEquals(2, in.readInt(), "tags长度");
        assertEquals("tag1", in.readStringInPool());
        assertEquals("tag2", in.readStringInPool());
        in.requireEnd();
    }

    @Test
    void roundtrip_namedBlocksRetainPackageLocalPools() throws IOException {
        Path output = tempDir.resolve("blocks");
        Context ctx = TestCtx.newContext(tempDir, CFG, java.util.Map.of("user", CSV));
        new BytesGenerator(new ParameterParser("bytes,dir:" + output + ",pack:base")).generate(ctx);
        var pack = ConfigPackage.read(new ConfigInput(output.resolve("config.bytes")));
        assertEquals("base", pack.name());
        org.junit.jupiter.api.Assertions.assertTrue(pack.blockNames().contains("schema"));
        String inspected = new configgen.genjava.BytesInspector(output.resolve("config.bytes").toString()).match("user");
        org.junit.jupiter.api.Assertions.assertTrue(inspected.contains("Alice"));
        org.junit.jupiter.api.Assertions.assertTrue(inspected.contains("你好世界"));
        try (ConfigInput in = pack.block(".user")) {
            assertEquals(1, in.readInt());
            assertEquals(1, in.readInt());
            assertEquals("Alice", in.readStringInPool());
            assertEquals("你好世界", in.readTextInPool());
            assertEquals(2, in.readInt());
            assertEquals("tag1", in.readStringInPool());
            assertEquals("tag2", in.readStringInPool());
            in.requireEnd();
        }
    }

    @Test
    void tableNamedSchemaDoesNotCollideWithSchemaBlock() throws IOException {
        var ctx = TestCtx.newContext(tempDir, "table schema[id] { id:int; }",
                java.util.Map.of("schema", "id\nid\n7\n"));
        new BytesGenerator(new ParameterParser("bytes,dir:" + tempDir.resolve("schema"))).generate(ctx);
        var pack = ConfigPackage.read(new ConfigInput(tempDir.resolve("schema/config.bytes")));
        assertEquals(java.util.Set.of("schema", "strings", "text/default", ".schema"), pack.blockNames());
        var table = pack.block(".schema");
        assertEquals(1, table.readInt());
        assertEquals(7, table.readInt());
        table.requireEnd();
        var schema = pack.block("schema");
        assertInstanceOf(SchemaInterface.class, SchemaDeserializer.deserialize(schema));
        schema.requireEnd();
    }
}
