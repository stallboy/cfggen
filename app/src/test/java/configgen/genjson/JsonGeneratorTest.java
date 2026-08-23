package configgen.genjson;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Json 生成器冒烟：按表名导出记录为每条一个json文件（_&lt;table&gt;/&lt;pk&gt;.json），
 * 内容可用 fastjson2 解析回读。
 */
class JsonGeneratorTest {

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
    void generate_tablesToJson() throws IOException {
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                    tags:list<str> (sep='|');
                }
                """;
        String csv = """
                用户ID,名字,标签
                id,name,tags
                1,Alice,a|b
                2,Bob,c
                """;

        Context ctx = TestCtx.newContext(tempDir, cfgStr, Map.of("user", csv));
        Path outDir = tempDir.resolve("jsonout");
        new JsonGenerator(new ParameterParser("json,tables:user,dst:" + outDir)).generate(ctx);

        Path recordJson = outDir.resolve("_user/1.json");
        assertTrue(Files.exists(recordJson), "应生成_user/1.json（每条记录一个文件）");

        String raw = Files.readString(recordJson);
        JSONObject record = JSON.parseObject(raw);
        assertEquals(1, record.getIntValue("id"));
        assertEquals("Alice", record.getString("name"));
        assertEquals(List.of("a", "b"), record.getJSONArray("tags").toJavaList(String.class));
    }
}
