package configgen.ctx;

import configgen.Resources;
import configgen.util.Logger;
import configgen.value.CfgValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ContextTest {

    private @TempDir Path tempDir;

    @BeforeAll
    static void setupLogger() {
        Logger.setPrinter(Logger.Printer.nullPrinter);
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
        Context ctx = new Context(tempDir);
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
        Context ctx = new Context(tempDir);

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

        Context ctx = new Context(tempDir);

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

        Context ctx = new Context(tempDir);

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

        Context ctx = new Context(tempDir);

        // When: 使用null tag生成配置值
        CfgValue nullTagValue = ctx.makeValue(null);
        CfgValue noTagValue = ctx.makeValue();

        // Then: 两者应该返回相同的配置值（无过滤）
        assertNotNull(nullTagValue);
        assertNotNull(noTagValue);
        assertEquals(nullTagValue.getTable("user").valueList().size(),
                     noTagValue.getTable("user").valueList().size());
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

        Context ctx = new Context(tempDir);

        // When & Then: 使用空字符串作为tag应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            ctx.makeValue("");
        });
    }

    @Test
    void shouldSupportCopyOperation() {
        // Given: 有效的配置目录和Context
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

        Context originalCtx = new Context(tempDir);

        // When: 复制Context
        Context copiedCtx = originalCtx.copy();

        // Then: 复制的Context应该具有相同的配置但独立的缓存
        assertNotNull(copiedCtx);
        assertNotSame(originalCtx, copiedCtx);

        CfgValue originalValue = originalCtx.makeValue("test");
        CfgValue copiedValue = copiedCtx.makeValue("test");

        assertNotSame(originalValue, copiedValue);
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

        Context ctx = new Context(tempDir);

        // When: 使用allowErr=true生成配置值
        CfgValue cfgValue = ctx.makeValue(null, true);

        // Then: 即使有错误，配置值也应该生成
        assertNotNull(cfgValue);
    }

    @Test
    void shouldReturnNullForNullableLangTextFinderWhenNotConfigured() {
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

        Context ctx = new Context(tempDir);

        // When & Then: 没有配置国际化时应该返回null
        assertNull(ctx.nullableLangTextFinder());
        assertNull(ctx.nullableLangSwitch());
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

        Context ctx = new Context(tempDir);

        // When: 获取Context配置
        Context.ContextCfg cfg = ctx.getContextCfg();

        // Then: 配置信息应该正确返回
        assertNotNull(cfg);
        assertEquals(tempDir, cfg.dataDir());
        assertNotNull(cfg.headRow());
        assertNotNull(cfg.csvOrTsvDefaultEncoding());
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
        Context ctx = new Context(tempDir);
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