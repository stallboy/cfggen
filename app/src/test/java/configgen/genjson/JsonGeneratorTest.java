package configgen.genjson;

import configgen.util.CachedFiles;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.Parameter;
import configgen.gen.ParameterParser;
import configgen.gengo.GoCodeGenerator;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void reexportUpdatesRecordsAndCleansMarkedDirectory() throws IOException {
        String schema = "table item[id] { id:int; score:int; }";
        String headers = "ID,Score\nid,score\n";
        Path data = dataDir();
        Path out = Files.createDirectory(tempDir.resolve("out"));
        var generator = new JsonGenerator(new ParameterParser("json,tables:item,dst:" + out));
        Path tableDir = out.resolve("_item");
        generateAndFinalize(generator, TestCtx.newContext(data, schema, Map.of("item", headers + "1,10\n2,20\n")));
        Path extra = tableDir.resolve("extra.txt");
        Files.writeString(extra, "obsolete");

        StringBuilder log = new StringBuilder();
        try (var ignored = Logger.printerScope((format, args) -> log.append(format.formatted(args)))) {
            generateAndFinalize(generator, TestCtx.newContext(data, schema, Map.of("item", headers + "1,11\n")));
        }
        assertEquals(11, JSON.parseObject(Files.readString(tableDir.resolve("1.json"))).getIntValue("score"));
        assertFalse(Files.exists(tableDir.resolve("2.json")));
        assertFalse(Files.exists(extra));
        assertTrue(log.toString().contains("modify file:"));
        assertTrue(log.toString().contains("delete file:"));

        generateAndFinalize(generator, TestCtx.newContext(data, schema, Map.of("item", headers)));
        assertFalse(Files.exists(tableDir.resolve("1.json")));
        assertTrue(Files.isRegularFile(out.resolve(".cfggen-output")));
        assertFalse(Files.exists(tableDir));
    }

    @Test
    void exportsQualifiedAndUnqualifiedTableNamesToDistinctDirectories() throws IOException {
        Context context = newContextWithQualifiedTable();
        Path out = tempDir.resolve("out");
        var generator = new JsonGenerator(new ParameterParser("json,tables:a_b;a.b,dst:" + out));

        generateAndFinalize(generator, context);
        assertTrue(Files.exists(out.resolve("_a_b/1.json")));
        assertTrue(Files.exists(out.resolve("a/_b/2.json")));

        Files.writeString(context.rootDir().resolve("a/b.csv"), "id\nid\n3\n");
        generateAndFinalize(generator, Context.load(context.rootDir()));
        assertTrue(Files.exists(out.resolve("_a_b/1.json")));
        assertTrue(Files.exists(out.resolve("a/_b/3.json")));
        assertFalse(Files.exists(out.resolve("a/_b/2.json")));
        assertFalse(Files.exists(out.resolve("_a_b/2.json")));
    }

    @Test
    void removesTablesOmittedFromTheNextExportToAMarkedDirectory() throws IOException {
        Context context = newContextWithQualifiedTable();
        Path out = tempDir.resolve("out");
        generateAndFinalize(new JsonGenerator(new ParameterParser("json,tables:a_b;a.b,dst:" + out)), context);
        assertTrue(Files.exists(out.resolve("_a_b/1.json")));
        assertTrue(Files.exists(out.resolve("a/_b/2.json")));
        Files.writeString(out.resolve("extra.txt"), "obsolete");

        generateAndFinalize(new JsonGenerator(new ParameterParser("json,tables:a.b,dst:" + out)), context);

        assertFalse(Files.exists(out.resolve("_a_b")));
        assertFalse(Files.exists(out.resolve("extra.txt")));
        assertTrue(Files.exists(out.resolve("a/_b/2.json")));
        assertTrue(Files.isRegularFile(out.resolve(".cfggen-output")));
        assertFalse(Files.exists(out.resolve("a/_b/.cfggen-output")));
    }

    @Test
    void preservesGoAndJsonOutputsInTheSameBatch() throws IOException {
        Context context = TestCtx.newContext(dataDir(), "table item[id] { id:int; }",
                Map.of("item", "id\nid\n1\n"));
        Path project = tempDir.resolve("go-project");
        Path output = project.resolve("config");
        CachedFiles outputs = new CachedFiles();

        new GoCodeGenerator(new ParameterParser("go,dir:" + project)).generate(context, outputs);
        Path stale = Files.writeString(output.resolve("stale.go"), "old");
        new JsonGenerator(new ParameterParser("json,tables:item,dst:" + output)).generate(context, outputs);
        assertTrue(Files.isRegularFile(stale));

        outputs.finalizeRun();

        assertTrue(Files.isRegularFile(output.resolve("item.go")));
        assertTrue(Files.isRegularFile(output.resolve("_item/1.json")));
        assertFalse(Files.exists(stale));
    }

    @Test
    void combinesJsonExportsToTheSameDirectoryInABatch() throws IOException {
        Context context = newContextWithQualifiedTable();
        Path output = tempDir.resolve("out");
        CachedFiles outputs = new CachedFiles();

        new JsonGenerator(new ParameterParser("json,tables:a_b,dst:" + output)).generate(context, outputs);
        new JsonGenerator(new ParameterParser("json,tables:a.b,dst:" + output)).generate(context, outputs);
        outputs.finalizeRun();

        assertTrue(Files.isRegularFile(output.resolve("_a_b/1.json")));
        assertTrue(Files.isRegularFile(output.resolve("a/_b/2.json")));
    }

    private static void generateAndFinalize(JsonGenerator generator, Context context) throws IOException {
        CachedFiles outputs = new CachedFiles();
        generator.generate(context, outputs);
        outputs.finalizeRun();
    }

    @Test
    void rejectsNonEmptyUnmarkedDirectoryBeforeWritingRecords() throws IOException {
        Context context = TestCtx.newContext(dataDir(), "table item[id] { id:int; }",
                Map.of("item", "id\nid\n1\n2\n"));
        Path out = Files.createDirectory(tempDir.resolve("out"));
        Path manual = out.resolve("notes.txt");
        Files.writeString(manual, "manual");
        var generator = new JsonGenerator(new ParameterParser("json,tables:item,dst:" + out));

        IOException error = assertThrows(IOException.class, () -> generator.generate(context, new CachedFiles()));
        assertTrue(error.getMessage().contains("has no regular output marker"));
        assertEquals("manual", Files.readString(manual));
        assertFalse(Files.exists(out.resolve("_item")));
        assertFalse(Files.exists(out.resolve(".cfggen-output")));
    }

    @Test
    void rejectsAFileBlockingAnOutputDirectoryBeforeWritingAnyTable() throws IOException {
        Context context = newContextWithQualifiedTable();
        Path out = Files.createDirectory(tempDir.resolve("out"));
        Files.createFile(out.resolve(".cfggen-output"));
        Path blocker = out.resolve("a");
        Files.writeString(blocker, "manual");
        var generator = new JsonGenerator(new ParameterParser("json,tables:a_b;a.b,dst:" + out));

        IOException error = assertThrows(IOException.class, () -> generator.generate(context, new CachedFiles()));
        assertTrue(error.getMessage().contains("occupied by a file"));
        assertTrue(error.getMessage().contains(blocker.toString()));
        assertEquals("manual", Files.readString(blocker));
        assertFalse(Files.exists(out.resolve("_a_b")));
    }

    private Context newContextWithQualifiedTable() throws IOException {
        Path data = dataDir();
        Files.createDirectory(data.resolve("a"));
        return TestCtx.newContext(data, "table a_b[id] { id:int; }\ntable a.b[id] { id:int; }",
                Map.of("a_b", "id\nid\n1\n", "a/b", "id\nid\n2\n"));
    }

    private Path dataDir() throws IOException {
        return Files.createDirectory(tempDir.resolve("data"));
    }

    @Test
    void exportingIntoSourceDirectoryFailsWithoutChangingSource() throws IOException {
        Path tableDir = Files.createDirectory(tempDir.resolve("_item"));
        Path original = tableDir.resolve("custom.json");
        String contents = "{\"id\":1,\"serverScore\":42}";
        Files.writeString(original, contents);
        Context context = TestCtx.newContext(tempDir,
                "table item[id] (json) { id:int (client); serverScore:int; }", Map.of());
        String parameters = "json,tables:item,dst:" + tempDir;
        for (String suffix : List.of("", ",own:client")) {
            var generator = new JsonGenerator(new ParameterParser(parameters + suffix));
            IOException error = assertThrows(IOException.class, () -> generator.generate(context, new CachedFiles()));
            assertTrue(error.getMessage().contains("Set dst outside the data directory"));
        }
        assertEquals(contents, Files.readString(original));
        try (var files = Files.list(tableDir)) {
            assertEquals(List.of(original), files.toList());
        }
    }

    @Test
    void rejectsCsvExportIntoANewSubdirectoryOfTheDataDirectory() throws IOException {
        Path data = dataDir();
        Context context = TestCtx.newContext(data, "table item[id] { id:int; }", Map.of("item", "id\nid\n1\n"));
        Path destination = data.resolve("generated/json");
        var generator = new JsonGenerator(new ParameterParser("json,tables:item,dst:" + destination));
        IOException error = assertThrows(IOException.class, () -> generator.generate(context, new CachedFiles()));
        assertTrue(error.getMessage().contains("Set dst outside the data directory"));
        assertFalse(Files.exists(data.resolve("generated")));
    }

    @Test
    void rejectsAnOutputTableDirectoryEqualToTheDataDirectory() throws IOException {
        Path data = Files.createDirectory(tempDir.resolve("_item"));
        Context context = TestCtx.newContext(data, "table item[id] { id:int; }", Map.of("item", "id\nid\n1\n"));
        var generator = new JsonGenerator(new ParameterParser("json,tables:item,dst:" + tempDir));

        assertThrows(IOException.class, () -> generator.generate(context, new CachedFiles()));
        assertFalse(Files.exists(data.resolve("1.json")));
    }

    @Test
    void rejectsMarkedOutputDirectoryContainingTheDataDirectory() throws IOException {
        Path output = Files.createDirectory(tempDir.resolve("_item"));
        Path data = Files.createDirectory(output.resolve("data"));
        Files.createFile(output.resolve(".cfggen-output"));
        Context context = TestCtx.newContext(data, "table item[id] { id:int; }", Map.of("item", "id\nid\n1\n"));
        var generator = new JsonGenerator(new ParameterParser("json,tables:item,dst:" + output));

        IOException error = assertThrows(IOException.class, () -> generator.generate(context, new CachedFiles()));
        assertTrue(error.getMessage().contains("overlaps the data directory"));
        assertTrue(Files.exists(data.resolve("item.csv")));
        assertFalse(Files.exists(output.resolve("_item")));
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

        Context ctx = TestCtx.newContext(dataDir(), cfgStr, Map.of("user", csv));
        Path outDir = tempDir.resolve("data-out");
        generateAndFinalize(new JsonGenerator(new ParameterParser("json,tables:user,dst:" + outDir)), ctx);

        Path recordJson = outDir.resolve("_user/1.json");
        assertTrue(Files.exists(recordJson), "应生成_user/1.json（每条记录一个文件）");

        String raw = Files.readString(recordJson);
        JSONObject record = JSON.parseObject(raw);
        assertEquals(1, record.getIntValue("id"));
        assertEquals("Alice", record.getString("name"));
        assertEquals(List.of("a", "b"), record.getJSONArray("tags").toJavaList(String.class));
    }
}
