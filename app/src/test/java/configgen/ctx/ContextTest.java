package configgen.ctx;

import configgen.Resources;
import configgen.TestCtx;
import configgen.schema.CfgSchemaErrs;
import configgen.schema.CfgSchemaException;
import configgen.schema.cfg.CfgReader;
import configgen.util.Logger;
import configgen.value.CfgValue;
import configgen.value.CfgValueException;
import configgen.write.AddOrUpdateService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

class ContextTest {

    @Test
    void strictValidationStillChecksOtherTablesAfterAnEdit(@TempDir Path data) {
        var context = TestCtx.newContext(data, """
                table broken[id] { id:int; score:int; }
                table valid[id] { id:int; score:int; }
                """, Map.of(
                "broken", "ID,Score\nid,score\n1,oops\n",
                "valid", "ID,Score\nid,score\n1,5\n"));
        var edit = AddOrUpdateService.addOrUpdateRecord(
                context, context.makeValue(null, true), "valid", "{\"id\":1,\"score\":7}");
        assertEquals(AddOrUpdateService.AddOrUpdateErrorCode.UpdateOK, edit.errorCode());
        assertTrue(edit.errorMessages().isEmpty());
        assertThrows(CfgValueException.class, () -> context.makeValue(null, false));
    }

    private @TempDir Path tempDir;

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
    }

    @AfterAll
    static void setDefaultLogger(){
        Logger.setPrinter(Logger.Printer.outPrinter);
    }


    @Test
    void reloadKeepsTheResolvedSharedSchemaForSparseData() throws Exception {
        var schema = CfgReader.parse("table user[id] { id:int; } table other[id] { id:int; }");
        schema.resolve().checkErrors();
        var context = Context.load(Context.ContextCfg.of(tempDir), schema);
        Files.writeString(tempDir.resolve("user.csv"), "ID\nid\n1\n");

        Context.LoadResult load = context.reloadFrom(context.sourceStructure().reload());
        assertFalse(load.schemaRewritten());
        Context reloaded = load.context();

        assertTrue(reloaded.usesSharedSchema());
        assertSame(schema, reloaded.cfgSchema());
        assertEquals(1, requireNonNull(reloaded.makeValue().getTable("user")).valueList().size());
        assertFalse(Files.exists(tempDir.resolve("config.cfg")));
    }

    @Test
    void loadAndReloadKeepTheSchemaAsWrittenWhenHeadersDiffer() throws Exception {
        String schemaText = "table user[id] { id:int; }";
        Path schema = tempDir.resolve("config.cfg");
        Path csv = tempDir.resolve("user.csv");
        Files.writeString(schema, schemaText);
        Files.writeString(csv, "ID,Extra\nid,extra\n1,ignored\n");

        Context context = Context.load(tempDir);
        assertFalse(context.usesSharedSchema());
        assertEquals(List.of("id"), requireNonNull(context.cfgSchema().findTable("user"))
                .fields().stream().map(field -> field.name()).toList());
        assertEquals(schemaText, Files.readString(schema));

        Files.writeString(csv, "Identifier,Extra\nid,extra\n1,ignored\n2,also ignored\n");
        Context.LoadResult reloaded = context.reloadFrom(context.sourceStructure().reload());
        assertFalse(reloaded.schemaRewritten());
        assertEquals(context.cfgSchema(), reloaded.context().cfgSchema());
        assertEquals(2, requireNonNull(reloaded.context().cfgData().getDTable("user")).rows().size());
        assertEquals(schemaText, Files.readString(schema));
    }

    @Test
    void reloadReportsSchemaRewriteOnlyForTheLoadThatPerformedIt() throws Exception {
        Path schema = tempDir.resolve("config.cfg");
        Path csv = tempDir.resolve("user.csv");
        Files.writeString(schema, "table user[id] { id:int; }");
        Files.writeString(csv, "ID\nid\n1\n");
        Context context = Context.loadAndAlignSchema(Context.ContextCfg.of(tempDir));

        Files.writeString(csv, "Identifier\nid\n1\n");
        Context.LoadResult fixed = context.reloadFrom(context.sourceStructure().reload());

        assertTrue(fixed.schemaRewritten());
        assertTrue(Files.readString(schema).contains("Identifier"));
        Context reloaded = fixed.context();
        assertFalse(reloaded.reloadFrom(reloaded.sourceStructure().reload()).schemaRewritten());
    }

    @Test
    void fixedSchemaReportsHeaderAndDataSourceErrorsTogether() throws Exception {
        String schemaText = """
                table normal[id] { id:int; }
                table jsononly[id] (json) { id:int; }
                """;
        var schema = CfgReader.parse(schemaText);
        schema.resolve().checkErrors();
        Files.writeString(tempDir.resolve("normal.csv"), "ID\nid\n1\n");
        Files.writeString(tempDir.resolve("normal_1.csv"), "Other\nother\n2\n");
        Files.writeString(tempDir.resolve("unknown.csv"), "ID\nid\n1\n");
        Files.writeString(tempDir.resolve("jsononly.csv"), "ID\nid\n1\n");
        for (String table : List.of("normal", "undeclared")) {
            Path dir = Files.createDirectory(tempDir.resolve("_" + table));
            Files.writeString(dir.resolve("1.json"), "{\"id\":1}");
        }
        Files.createDirectory(tempDir.resolve("_empty"));

        var failure = assertThrows(CfgSchemaException.class,
                () -> Context.load(Context.ContextCfg.of(tempDir), schema));
        var errs = failure.getErrs().errs();
        assertEquals(5, errs.size());
        assertTrue(errs.stream().anyMatch(CfgSchemaErrs.SplitDataHeaderNotEqual.class::isInstance));
        assertTrue(errs.contains(new CfgSchemaErrs.DataTableNotInSchema("unknown",
                List.of("unknown.csv"))));
        assertTrue(errs.contains(new CfgSchemaErrs.JsonTableNotSupportExcel("jsononly",
                List.of("jsononly.csv"))));
        assertTrue(errs.contains(new CfgSchemaErrs.DataTableNotMarkedJson("normal",
                List.of(Path.of("_normal", "1.json").toString()))));
        assertTrue(errs.contains(new CfgSchemaErrs.DataTableNotInSchema("undeclared",
                List.of(Path.of("_undeclared", "1.json").toString()))));

        Files.writeString(tempDir.resolve("config.cfg"), schemaText);
        var localFailure = assertThrows(CfgSchemaException.class, () -> Context.load(tempDir));
        assertEquals(errs, localFailure.getErrs().errs());
    }

    @Test
    void shouldCreateCfgValueFromValidConfigDirectory() {
        // Given: 有效的配置目录，包含schema和数据文件
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                    age:int;
                }
                """;

        String csvData = """
                用户ID,姓名,年龄
                id,name,age
                1,Alice,25
                2,Bob,30
                3,Charlie,35
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // When: 创建Context并生成配置值
        Context ctx = Context.load(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        // Then: 验证配置值正确生成
        assertNotNull(cfgValue);
        assertNotNull(ctx.cfgSchema());
        assertNotNull(ctx.cfgData());

        var userTable = cfgValue.getTable("user");
        assertNotNull(userTable);
        assertEquals(3, userTable.valueList().size());
    }

    @Test
    void shouldHandleEmptyDirectoryGracefully() {
        // Given: 空目录

        // When: 创建Context
        Context ctx = Context.load(tempDir);

        // Then: Context应该成功创建，但Schema和Data可能为空
        assertNotNull(ctx);
        assertNotNull(ctx.cfgSchema());
        assertNotNull(ctx.cfgData());
        assertEquals(0, ctx.cfgData().tables().size());
    }

    @Test
    void shouldCacheCfgValueForSameTag() {
        // Given: 有效的配置目录
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                    age:int;
                }
                """;

        String csvData = """
                用户ID,姓名,年龄
                id,name,age
                1,Alice,25
                2,Bob,30
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        Context ctx = Context.load(tempDir);

        // When: 使用相同tag多次生成配置值
        String tag = "test-tag";
        CfgValue firstValue = ctx.makeValue(tag);
        CfgValue secondValue = ctx.makeValue(tag);

        // Then: 应该返回相同的对象实例（缓存生效）
        assertSame(firstValue, secondValue);
    }

    @Test
    void shouldGenerateDifferentCfgValueForDifferentTags() {
        // Given: 有效的配置目录
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                    age:int;
                }
                """;

        String csvData = """
                用户ID,姓名,年龄
                id,name,age
                1,Alice,25
                2,Bob,30
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        Context ctx = Context.load(tempDir);

        // When: 使用不同tag生成配置值
        CfgValue value1 = ctx.makeValue("tag1");
        CfgValue value2 = ctx.makeValue("tag2");

        // Then: 应该返回不同的对象实例
        assertNotSame(value1, value2);
    }

    @Test
    void shouldHandleNullTagAsNoFiltering() {
        // Given: 有效的配置目录
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                    age:int;
                }
                """;

        String csvData = """
                用户ID,姓名,年龄
                id,name,age
                1,Alice,25
                2,Bob,30
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        Context ctx = Context.load(tempDir);

        // When: 使用null tag生成配置值
        CfgValue nullTagValue = ctx.makeValue(null);
        CfgValue noTagValue = ctx.makeValue();

        // Then: 两者应该返回相同的配置值（无过滤）
        assertNotNull(nullTagValue);
        assertNotNull(noTagValue);
        assertEquals(requireNonNull(nullTagValue.getTable("user")).valueList().size(),
                     requireNonNull(noTagValue.getTable("user")).valueList().size());
    }

    @Test
    void shouldThrowExceptionForEmptyTag() {
        // Given: 有效的配置目录
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                    age:int;
                }
                """;

        String csvData = """
                用户ID,姓名,年龄
                id,name,age
                1,Alice,25
                2,Bob,30
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        Context ctx = Context.load(tempDir);

        // When & Then: 使用空字符串作为tag应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            ctx.makeValue("");
        });
    }

    @Test
    void shouldAllowErrorInMakeValueWhenRequested() {
        // Given: 包含错误的配置目录
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                    age:int;
                }
                """;

        // 故意创建格式错误的数据文件
        String invalidCsvData = """
                用户ID,姓名,年龄
                id,name,age
                1,Alice,invalid_age
                2,Bob,30
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, invalidCsvData);

        Context ctx = Context.load(tempDir);

        // When: 使用allowErr=true生成配置值
        CfgValue cfgValue = ctx.makeValue(null, true);

        // Then: 即使有错误，配置值也应该生成
        assertNotNull(cfgValue);
    }

    @Test
    void shouldKeyCacheByAllowErrSemantics() {
        // allowErr 不进入 cfgValue 内容，只控制 checkErrors 是否抛异常。缓存命中按"值是否可能带错"判断：
        //   严格值（allowErr=false 产生，保证无错）可服务任意请求；
        //   宽松值（allowErr=true 产生，可能带错）只能服务宽松请求。
        // 关键回归门：宽松→严格必须 miss 重算，否则严格请求复用宽松值、跳过 checkErrors 拿到本该报错的值。
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;

        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        Context ctx = Context.load(tempDir);

        String tag = "editor";

        // 宽松→严格：必须 miss 重算（不同对象）—— bug 回归门
        CfgValue lenient = ctx.makeValue(tag, true);
        CfgValue strict = ctx.makeValue(tag, false);
        assertNotSame(lenient, strict);

        // 严格→严格：命中缓存（同一对象）
        assertSame(strict, ctx.makeValue(tag, false));

        // 严格→宽松：也应命中（严格值保证无错，可安全服务宽松请求，不必重算）
        assertSame(strict, ctx.makeValue(tag, true));
    }

    @Test
    void shouldReturnNullForNullableTranslationCatalogWhenNotConfigured() {
        // Given: 没有配置国际化文件的Context
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                    age:int;
                }
                """;

        String csvData = """
                用户ID,姓名,年龄
                id,name,age
                1,Alice,25
                2,Bob,30
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        Context ctx = Context.load(tempDir);

        // When & Then: 没有配置国际化时应该返回null
        assertNull(ctx.translationCatalog());
        assertNull(ctx.translationBundle());
    }

    @Test
    void shouldProvideAccessToContextConfiguration() {
        // Given: 有效的配置目录
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                    age:int;
                }
                """;

        String csvData = """
                用户ID,姓名,年龄
                id,name,age
                1,Alice,25
                2,Bob,30
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        Context ctx = Context.load(tempDir);

        // When: 获取Context配置
        Context.ContextCfg cfg = ctx.contextCfg();

        // Then: 配置信息应该正确返回
        assertNotNull(cfg);
        assertEquals(tempDir, cfg.dataDir());
        assertNotNull(cfg.headRow());
        assertNotNull(cfg.csvDefaultEncoding());
    }

    @Test
    void shouldHandleComplexSchemaWithReferences() {
        // Given: 包含外键引用的复杂schema
        String cfgStr = """
                table department[id] {
                    id:int;
                    name:str;
                }

                table employee[id] {
                    id:int;
                    name:str;
                    department_id:int ->department;
                }
                """;

        String deptCsv = """
                部门ID,部门名称
                id,name
                1,Engineering
                2,Marketing
                """;

        String empCsv = """
                员工ID,姓名,部门ID
                id,name,department_id
                1,Alice,1
                2,Bob,2
                3,Charlie,1
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("department.csv", tempDir, deptCsv);
        Resources.addTempFileFromText("employee.csv", tempDir, empCsv);

        // When: 创建Context并生成配置值
        Context ctx = Context.load(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        // Then: 验证引用完整性
        assertNotNull(cfgValue);

        var deptTable = cfgValue.getTable("department");
        var empTable = cfgValue.getTable("employee");

        assertNotNull(deptTable);
        assertNotNull(empTable);
        assertEquals(2, deptTable.valueList().size());
        assertEquals(3, empTable.valueList().size());

        // 验证外键引用正确建立
        for (var employee : empTable.valueList()) {
            var deptId = employee.values().get(2); // department_id字段
            var department = deptTable.primaryKeyMap().get(deptId);
            assertNotNull(department, "Employee references non-existent department " + deptId);
        }
    }
}
