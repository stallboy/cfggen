package configgen;

import configgen.ctx.Context;
import configgen.schema.CfgSchema;
import configgen.schema.cfg.CfgReader;
import configgen.value.CfgValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCompleteWorkflowFromSchemaToValues() {
        // Given: 完整的配置定义和数据
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

        // When: 执行完整工作流
        // 1. 解析schema
        CfgSchema schema = CfgReader.parse(cfgStr);
        var resolutionErrors = schema.resolve();
        resolutionErrors.checkErrors();

        // 2. 创建Context并生成值
        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        // Then: 验证完整工作流成功
        assertNotNull(schema);
        assertNotNull(cfgValue);

        // 验证数据正确解析
        var userTable = cfgValue.getTable("user");
        assertNotNull(userTable);
        assertEquals(3, userTable.valueList().size());
    }

    @Test
    void shouldHandleComplexIntegrationScenario() {
        // Given: 复杂集成场景 - 包含外键引用
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

        // When: 执行完整工作流
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

        // 验证外键引用
        for (var employee : empTable.valueList()) {
            var deptId = employee.values().get(2); // department_id字段
            var department = deptTable.primaryKeyMap().get(deptId);
            assertNotNull(department, "Employee references non-existent department " + deptId);
        }
    }
}