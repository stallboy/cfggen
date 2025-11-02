package configgen.ctx;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 行为驱动测试：验证 ExplicitDir 类的公共行为
 * 专注于配置解析、目录映射、空值处理、参数验证等外部行为
 */
class ExplicitDirBehaviorTest {

    @Test
    void shouldCreateExplicitDirWithValidConfiguration() {
        // Given: 有效的配置参数
        Map<String, String> txtAsTsvMap = Map.of("client_tables", "noserver");
        Set<String> excelDirs = Set.of("excel_files");
        Set<String> jsonDirs = Set.of("json_data");

        // When: 创建 ExplicitDir
        ExplicitDir explicitDir = new ExplicitDir(txtAsTsvMap, excelDirs, jsonDirs);

        // Then: 应该成功创建
        assertNotNull(explicitDir, "应该成功创建 ExplicitDir");
        assertEquals(txtAsTsvMap, explicitDir.txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map());
        assertEquals(excelDirs, explicitDir.excelFileDirs());
        assertEquals(jsonDirs, explicitDir.jsonFileDirs());
    }

    @Test
    void shouldThrowExceptionWhenNullParametersAreProvided() {
        // Given: null 参数
        Map<String, String> validMap = Map.of("dir", "tag");
        Set<String> validSet = Set.of("dir");

        // When & Then: 应该为每个 null 参数抛出 NullPointerException
        assertThrows(NullPointerException.class, () -> {
            new ExplicitDir(null, validSet, validSet);
        }, "null txtAsTsvMap 应该抛出异常");

        assertThrows(NullPointerException.class, () -> {
            new ExplicitDir(validMap, null, validSet);
        }, "null excelFileDirs 应该抛出异常");

        assertThrows(NullPointerException.class, () -> {
            new ExplicitDir(validMap, validSet, null);
        }, "null jsonFileDirs 应该抛出异常");
    }

    @Test
    void shouldParseValidConfigurationStringToExplicitDir() {
        // Given: 有效的配置字符串
        String asRoot = "client_tables:noserver,public_tables:,server_tables:noclient";
        String excelDirs = "excel_files,data_sheets";
        String jsonDirs = "json_data,config_json";

        // When: 解析配置
        ExplicitDir explicitDir = ExplicitDir.parse(asRoot, excelDirs, jsonDirs);

        // Then: 应该成功解析
        assertNotNull(explicitDir, "应该成功解析配置");

        // 验证 txtAsTsv 映射
        Map<String, String> txtAsTsvMap = explicitDir.txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map();
        assertEquals("noserver", txtAsTsvMap.get("client_tables"));
        assertEquals("", txtAsTsvMap.get("public_tables"));
        assertEquals("noclient", txtAsTsvMap.get("server_tables"));

        // 验证 Excel 目录
        Set<String> excelDirSet = explicitDir.excelFileDirs();
        assertTrue(excelDirSet.contains("excel_files"));
        assertTrue(excelDirSet.contains("data_sheets"));

        // 验证 JSON 目录
        Set<String> jsonDirSet = explicitDir.jsonFileDirs();
        assertTrue(jsonDirSet.contains("json_data"));
        assertTrue(jsonDirSet.contains("config_json"));
    }

    @Test
    void shouldReturnNullWhenAllConfigurationParametersAreEmpty() {
        // Given: 所有配置参数都为空
        String emptyAsRoot = "";
        String emptyExcelDirs = "";
        String emptyJsonDirs = "";

        // When: 解析空配置
        ExplicitDir explicitDir = ExplicitDir.parse(emptyAsRoot, emptyExcelDirs, emptyJsonDirs);

        // Then: 应该返回 null
        assertNull(explicitDir, "所有参数为空时应该返回 null");
    }

    @Test
    void shouldParseConfigurationWithEmptyTagsInTxtAsTsvMapping() {
        // Given: 包含空标签的 txtAsTsv 映射
        String asRoot = "client_tables:noserver,public_tables:,server_tables:noclient";
        String excelDirs = "excel_files";
        String jsonDirs = "json_data";

        // When: 解析配置
        ExplicitDir explicitDir = ExplicitDir.parse(asRoot, excelDirs, jsonDirs);

        // Then: 应该成功解析空标签
        assertNotNull(explicitDir, "应该成功解析包含空标签的配置");

        Map<String, String> txtAsTsvMap = explicitDir.txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map();
        assertEquals("", txtAsTsvMap.get("public_tables"), "空标签应该被正确解析");
    }

    @Test
    void shouldParseConfigurationWithSingleDirectory() {
        // Given: 单个目录配置
        String asRoot = "client_tables:noserver";
        String excelDirs = "excel_files";
        String jsonDirs = "json_data";

        // When: 解析配置
        ExplicitDir explicitDir = ExplicitDir.parse(asRoot, excelDirs, jsonDirs);

        // Then: 应该成功解析
        assertNotNull(explicitDir, "应该成功解析单个目录配置");

        Set<String> excelDirSet = explicitDir.excelFileDirs();
        assertEquals(1, excelDirSet.size(), "应该包含一个 Excel 目录");
        assertTrue(excelDirSet.contains("excel_files"));

        Set<String> jsonDirSet = explicitDir.jsonFileDirs();
        assertEquals(1, jsonDirSet.size(), "应该包含一个 JSON 目录");
        assertTrue(jsonDirSet.contains("json_data"));
    }

    @Test
    void shouldParseConfigurationWithMultipleDirectories() {
        // Given: 多个目录配置
        String asRoot = "dir1:tag1,dir2:tag2,dir3:tag3";
        String excelDirs = "excel1,excel2,excel3";
        String jsonDirs = "json1,json2,json3";

        // When: 解析配置
        ExplicitDir explicitDir = ExplicitDir.parse(asRoot, excelDirs, jsonDirs);

        // Then: 应该成功解析所有目录
        assertNotNull(explicitDir, "应该成功解析多个目录配置");

        Map<String, String> txtAsTsvMap = explicitDir.txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map();
        assertEquals(3, txtAsTsvMap.size(), "应该包含 3 个 txtAsTsv 映射");

        Set<String> excelDirSet = explicitDir.excelFileDirs();
        assertEquals(3, excelDirSet.size(), "应该包含 3 个 Excel 目录");

        Set<String> jsonDirSet = explicitDir.jsonFileDirs();
        assertEquals(3, jsonDirSet.size(), "应该包含 3 个 JSON 目录");
    }

    @Test
    void shouldHandleConfigurationWithOnlyTxtAsTsvMapping() {
        // Given: 只有 txtAsTsv 映射的配置
        String asRoot = "client_tables:noserver";
        String emptyExcelDirs = "";
        String emptyJsonDirs = "";

        // When: 解析配置
        ExplicitDir explicitDir = ExplicitDir.parse(asRoot, emptyExcelDirs, emptyJsonDirs);

        // Then: 应该成功创建（因为至少有一个参数不为空）
        assertNotNull(explicitDir, "只有 txtAsTsv 映射时应该创建 ExplicitDir");

        Map<String, String> txtAsTsvMap = explicitDir.txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map();
        assertEquals(1, txtAsTsvMap.size(), "应该包含 1 个 txtAsTsv 映射");

        Set<String> excelDirSet = explicitDir.excelFileDirs();
        assertTrue(excelDirSet.isEmpty(), "Excel 目录应该为空");

        Set<String> jsonDirSet = explicitDir.jsonFileDirs();
        assertTrue(jsonDirSet.isEmpty(), "JSON 目录应该为空");
    }

    @Test
    void shouldHandleConfigurationWithOnlyExcelDirs() {
        // Given: 只有 Excel 目录的配置
        String emptyAsRoot = "";
        String excelDirs = "excel_files,data_sheets";
        String emptyJsonDirs = "";

        // When: 解析配置
        ExplicitDir explicitDir = ExplicitDir.parse(emptyAsRoot, excelDirs, emptyJsonDirs);

        // Then: 应该成功创建
        assertNotNull(explicitDir, "只有 Excel 目录时应该创建 ExplicitDir");

        Map<String, String> txtAsTsvMap = explicitDir.txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map();
        assertTrue(txtAsTsvMap.isEmpty(), "txtAsTsv 映射应该为空");

        Set<String> excelDirSet = explicitDir.excelFileDirs();
        assertEquals(2, excelDirSet.size(), "应该包含 2 个 Excel 目录");

        Set<String> jsonDirSet = explicitDir.jsonFileDirs();
        assertTrue(jsonDirSet.isEmpty(), "JSON 目录应该为空");
    }

    @Test
    void shouldHandleConfigurationWithOnlyJsonDirs() {
        // Given: 只有 JSON 目录的配置
        String emptyAsRoot = "";
        String emptyExcelDirs = "";
        String jsonDirs = "json_data,config_json";

        // When: 解析配置
        ExplicitDir explicitDir = ExplicitDir.parse(emptyAsRoot, emptyExcelDirs, jsonDirs);

        // Then: 应该成功创建
        assertNotNull(explicitDir, "只有 JSON 目录时应该创建 ExplicitDir");

        Map<String, String> txtAsTsvMap = explicitDir.txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map();
        assertTrue(txtAsTsvMap.isEmpty(), "txtAsTsv 映射应该为空");

        Set<String> excelDirSet = explicitDir.excelFileDirs();
        assertTrue(excelDirSet.isEmpty(), "Excel 目录应该为空");

        Set<String> jsonDirSet = explicitDir.jsonFileDirs();
        assertEquals(2, jsonDirSet.size(), "应该包含 2 个 JSON 目录");
    }

    @Test
    void shouldHandleConfigurationWithDuplicateDirectoryNames() {
        // Given: 包含重复目录名的配置
        String asRoot = "dir1:tag1,dir1:tag2"; // 重复的键
        String excelDirs = "excel,excel"; // 重复的值
        String jsonDirs = "json";

        // When: 解析配置
        ExplicitDir explicitDir = ExplicitDir.parse(asRoot, excelDirs, jsonDirs);

        // Then: 应该成功解析（重复的处理取决于 ArgParser 的实现）
        assertNotNull(explicitDir, "应该成功解析包含重复目录的配置");

        // 注意：重复键的处理取决于 Map 的实现，通常后面的值会覆盖前面的
        Map<String, String> txtAsTsvMap = explicitDir.txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map();
        // 这里不具体测试值，因为取决于解析器的行为

        Set<String> excelDirSet = explicitDir.excelFileDirs();
        // Set 会自动去重，所以应该只有一个元素
        assertEquals(1, excelDirSet.size(), "重复的 Excel 目录应该被去重");
    }

    @Test
    void shouldHandleConfigurationWithSpecialCharactersInTags() {
        // Given: 包含特殊字符的标签配置
        String asRoot = "client_tables:noserver-123,public_tables:tag_with_underscore";
        String excelDirs = "excel-files,data.sheets";
        String jsonDirs = "json-data,config.json";

        // When: 解析配置
        ExplicitDir explicitDir = ExplicitDir.parse(asRoot, excelDirs, jsonDirs);

        // Then: 应该成功解析特殊字符
        assertNotNull(explicitDir, "应该成功解析包含特殊字符的配置");

        Map<String, String> txtAsTsvMap = explicitDir.txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map();
        assertEquals("noserver-123", txtAsTsvMap.get("client_tables"));
        assertEquals("tag_with_underscore", txtAsTsvMap.get("public_tables"));

        Set<String> excelDirSet = explicitDir.excelFileDirs();
        assertTrue(excelDirSet.contains("excel-files"));
        assertTrue(excelDirSet.contains("data.sheets"));
    }

    @Test
    void shouldHandleEmptyStringsInConfigurationParameters() {
        // Given: 包含空字符串的配置参数
        String asRoot = "";
        String excelDirs = "";
        String jsonDirs = "json_data";

        // When: 解析配置
        ExplicitDir explicitDir = ExplicitDir.parse(asRoot, excelDirs, jsonDirs);

        // Then: 应该成功创建（因为至少有一个参数不为空）
        assertNotNull(explicitDir, "至少有一个非空参数时应该创建 ExplicitDir");

        Map<String, String> txtAsTsvMap = explicitDir.txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map();
        assertTrue(txtAsTsvMap.isEmpty(), "txtAsTsv 映射应该为空");

        Set<String> excelDirSet = explicitDir.excelFileDirs();
        assertTrue(excelDirSet.isEmpty(), "Excel 目录应该为空");

        Set<String> jsonDirSet = explicitDir.jsonFileDirs();
        assertEquals(1, jsonDirSet.size(), "应该包含 1 个 JSON 目录");
    }
}