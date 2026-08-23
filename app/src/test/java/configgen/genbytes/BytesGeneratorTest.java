package configgen.genbytes;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.Parameter;
import configgen.gen.ParameterParser;
import configgen.genjava.ConfigInput;
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
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * config.bytes 的生成→读回 roundtrip。读取顺序与生成的 ConfigMgrLoader 一致：
 * [schemaLen(+schema)] → StringPool → LangTextPool → 表数据（表数，{表名,大小,数据}）。
 */
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
    void roundtrip_withSchema_textReadFromLangTextPool() throws IOException {
        Path outDir = tempDir.resolve("out_schema");
        Context ctx = TestCtx.newContext(tempDir, CFG, java.util.Map.of("user", CSV));
        new BytesGenerator(new ParameterParser("bytes,dir:" + outDir + ",schema")).generate(ctx);

        try (ConfigInput in = new ConfigInput(outDir.resolve("config.bytes"))) {
            int schemaLen = in.readInt();
            org.junit.jupiter.api.Assertions.assertTrue(schemaLen > 0, "带schema参数时应有嵌入schema");
            SchemaInterface si = (SchemaInterface) SchemaDeserializer.deserialize(in);

            SchemaBean user = (SchemaBean) si.implementations.get("user");
            assertEquals(SStr, user.columns.get(1).schema(), "str字段应为SStr");
            // 回归：TEXT曾被标成SStr，按schema读数据会走StringPool读错池
            assertEquals(SText, user.columns.get(2).schema(), "text字段应为SText（读LangTextPool）");

            readAndAssertUserData(in);
        }
    }

    @Test
    void roundtrip_withoutSchema() throws IOException {
        Path outDir = tempDir.resolve("out_plain");
        Context ctx = TestCtx.newContext(tempDir, CFG, java.util.Map.of("user", CSV));
        new BytesGenerator(new ParameterParser("bytes,dir:" + outDir)).generate(ctx);

        try (ConfigInput in = new ConfigInput(outDir.resolve("config.bytes"))) {
            assertEquals(0, in.readInt(), "无schema参数时schemaLen应为0");
            readAndAssertUserData(in);
        }
    }

    private static void readAndAssertUserData(ConfigInput in) throws IOException {
        in.readStringPool();
        in.readLangTextPool();

        assertEquals(1, in.readInt(), "表数量");
        assertEquals("user", in.readString());
        int tableSize = in.readInt();
        org.junit.jupiter.api.Assertions.assertTrue(tableSize > 0);

        assertEquals(1, in.readInt(), "记录数");
        assertEquals(1, in.readInt(), "id");
        assertEquals("Alice", in.readStringInPool(), "str字段走StringPool");
        assertEquals("你好世界", in.readTextInPool(), "text字段走LangTextPool");
        assertEquals(2, in.readInt(), "tags长度");
        assertEquals("tag1", in.readStringInPool());
        assertEquals("tag2", in.readStringInPool());
    }
}
