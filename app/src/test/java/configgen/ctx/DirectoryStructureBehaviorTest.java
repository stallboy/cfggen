package configgen.ctx;

import configgen.Resources;
import configgen.util.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 行为驱动测试：验证 DirectoryStructure 类的公共行为
 * 专注于文件发现、JSON操作、状态比较等外部行为
 */
class DirectoryStructureBehaviorTest {

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
    void shouldDiscoverConfigurationFilesWhenDirectoryContainsValidConfigFiles() throws IOException {
        // Given: 包含有效配置文件的目录结构
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                }
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);

        // When: 创建 DirectoryStructure
        DirectoryStructure structure = new DirectoryStructure(tempDir);

        // Then: 应该发现配置文件
        Collection<DirectoryStructure.CfgFileInfo> cfgFiles = structure.getCfgFiles();
        assertFalse(cfgFiles.isEmpty(), "应该发现至少一个配置文件");

        DirectoryStructure.CfgFileInfo configFile = cfgFiles.iterator().next();
        assertEquals("config.cfg", configFile.relativePath().toString());
        assertTrue(configFile.lastModified() > 0);
    }

    @Test
    void shouldDiscoverExcelFilesWhenDirectoryContainsValidExcelFiles() throws IOException {
        // Given: 包含 Excel 文件的目录
        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                2,Bob
                """;

        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // When: 创建 DirectoryStructure
        DirectoryStructure structure = new DirectoryStructure(tempDir);

        // Then: 应该发现 Excel 文件
        Collection<DirectoryStructure.ExcelFileInfo> excelFiles = structure.getExcelFiles();
        assertFalse(excelFiles.isEmpty(), "应该发现至少一个 Excel 文件");

        DirectoryStructure.ExcelFileInfo excelFile = excelFiles.iterator().next();
        assertEquals("user.csv", excelFile.relativePath().toString());
        assertTrue(excelFile.lastModified() > 0);
    }

    @Test
    void shouldDiscoverJsonFilesWhenDirectoryContainsValidJsonFiles() throws IOException {
        // Given: 包含 JSON 文件的目录结构
        Path jsonDir = tempDir.resolve("_user");
        Files.createDirectories(jsonDir);

        String jsonData = "{\"id\": 1, \"name\": \"Alice\"}";
        Resources.addTempFileFromText("1.json", jsonDir, jsonData);

        // When: 创建 DirectoryStructure
        DirectoryStructure structure = new DirectoryStructure(tempDir);

        // Then: 应该发现 JSON 文件
        Collection<DirectoryStructure.JsonFileInfo> jsonFiles = structure.getJsonFilesByTable("user");
        assertFalse(jsonFiles.isEmpty(), "应该发现至少一个 JSON 文件");

        DirectoryStructure.JsonFileInfo jsonFile = jsonFiles.iterator().next();
        assertTrue(jsonFile.relativePath().toString().endsWith("_user/1.json") ||
                   jsonFile.relativePath().toString().endsWith("_user\\1.json"));
        assertTrue(jsonFile.lastModified() > 0);
    }

    @Test
    void shouldReturnEmptyCollectionWhenNoJsonFilesExistForTable() {
        // Given: 不包含任何 JSON 文件的目录

        // When: 创建 DirectoryStructure
        DirectoryStructure structure = new DirectoryStructure(tempDir);

        // Then: 应该返回空的 JSON 文件集合
        Collection<DirectoryStructure.JsonFileInfo> jsonFiles = structure.getJsonFilesByTable("nonexistent");
        assertTrue(jsonFiles.isEmpty(), "对于不存在的表应该返回空集合");
    }

    @Test
    void shouldAddJsonFileWhenValidJsonFileIsAddedToTableDirectory() throws IOException {
        // Given: 包含 JSON 目录结构的 DirectoryStructure
        Path jsonDir = tempDir.resolve("_user");
        Files.createDirectories(jsonDir);

        DirectoryStructure structure = new DirectoryStructure(tempDir);

        // When: 添加新的 JSON 文件
        Path newJsonFile = jsonDir.resolve("2.json");
        String jsonData = "{\"id\": 2, \"name\": \"Bob\"}";
        Files.writeString(newJsonFile, jsonData);

        DirectoryStructure.JsonFileInfo addedFile = structure.addJsonFile("user", Path.of("_user/2.json"));

        // Then: 应该成功添加 JSON 文件
        assertNotNull(addedFile, "应该返回添加的 JSON 文件信息");
        assertTrue(addedFile.relativePath().toString().endsWith("_user/2.json") ||
                   addedFile.relativePath().toString().endsWith("_user\\2.json"));

        Collection<DirectoryStructure.JsonFileInfo> jsonFiles = structure.getJsonFilesByTable("user");
        assertEquals(1, jsonFiles.size(), "应该包含新添加的 JSON 文件");
    }

    @Test
    void shouldRemoveJsonFileWhenJsonFileIsRemovedFromTableDirectory() throws IOException {
        // Given: 包含 JSON 文件的 DirectoryStructure
        Path relativePath = Path.of("_user/1.json");
        Path jsonDir = tempDir.resolve("_user");
        Files.createDirectories(jsonDir);

        String jsonData = "{\"id\": 1, \"name\": \"Alice\"}";
        Path jsonFile = tempDir.resolve(relativePath);
        Files.writeString(jsonFile, jsonData);

        DirectoryStructure structure = new DirectoryStructure(tempDir);

        // When: 移除 JSON 文件
        structure.removeJsonFile("user", relativePath);

        // Then: 应该成功移除 JSON 文件
        Collection<DirectoryStructure.JsonFileInfo> jsonFiles = structure.getJsonFilesByTable("user");
        assertTrue(jsonFiles.isEmpty(), "移除后应该返回空集合");
    }

    @Test
    void shouldReloadDirectoryStructureWhenFilesAreModified() throws IOException {
        // Given: 初始的 DirectoryStructure
        String initialCfg = """
                table user[id] {
                    id:int;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, initialCfg);

        DirectoryStructure originalStructure = new DirectoryStructure(tempDir);

        // When: 重新加载目录结构
        DirectoryStructure reloadedStructure = originalStructure.reload();

        // Then: 应该返回新的 DirectoryStructure 实例
        assertNotSame(originalStructure, reloadedStructure, "重新加载应该返回新实例");
        assertNotNull(reloadedStructure, "重新加载的实例不应该为 null");
    }

    @Test
    void shouldReturnTrueWhenLastModifiedTimesAreEqual() throws IOException {
        // Given: 两个相同的 DirectoryStructure
        String cfgStr = """
                table user[id] {
                    id:int;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);

        DirectoryStructure structure1 = new DirectoryStructure(tempDir);
        DirectoryStructure structure2 = new DirectoryStructure(tempDir);

        // When: 比较最后修改时间
        boolean areEqual = structure1.lastModifiedEquals(structure2);

        // Then: 应该返回 true
        assertTrue(areEqual, "相同目录结构的最后修改时间应该相等");
    }

    @Test
    void shouldReturnFalseWhenLastModifiedTimesAreDifferent() throws IOException, InterruptedException {
        // Given: 两个 DirectoryStructure，其中一个有更新的文件
        String cfgStr = """
                table user[id] {
                    id:int;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);

        DirectoryStructure structure1 = new DirectoryStructure(tempDir);

        // 等待一小段时间确保时间戳不同
        Thread.sleep(10);

        // 添加新文件
        String csvData = """
                用户ID
                id
                1
                """;
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        DirectoryStructure structure2 = new DirectoryStructure(tempDir);

        // When: 比较最后修改时间
        boolean areEqual = structure1.lastModifiedEquals(structure2);

        // Then: 应该返回 false
        assertFalse(areEqual, "不同目录结构的最后修改时间应该不相等");
    }

    @Test
    void shouldIgnoreHiddenFilesWhenScanningDirectory() throws IOException {
        // Given: 包含隐藏文件的目录
        String cfgStr = """
                table user[id] {
                    id:int;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);

        // 创建隐藏文件（以 ~ 开头）
        Path hiddenFile = tempDir.resolve("~temp.csv");
        Files.writeString(hiddenFile, "hidden content");

        // When: 创建 DirectoryStructure
        DirectoryStructure structure = new DirectoryStructure(tempDir);

        // Then: 应该忽略隐藏文件
        Collection<DirectoryStructure.ExcelFileInfo> excelFiles = structure.getExcelFiles();
        assertTrue(excelFiles.isEmpty(), "应该忽略隐藏文件");

        // 但应该发现配置文件
        Collection<DirectoryStructure.CfgFileInfo> cfgFiles = structure.getCfgFiles();
        assertFalse(cfgFiles.isEmpty(), "应该发现配置文件");
    }

    @Test
    void shouldHandleExplicitDirectoryConfigurationWhenExplicitDirIsProvided() throws IOException {
        // Given: 显式目录配置
        Path excelDir = tempDir.resolve("excel_files");
        Files.createDirectories(excelDir);

        String csvData = """
                用户ID,姓名
                id,name
                1,Alice
                """;
        Resources.addTempFileFromText("user.csv", excelDir, csvData);

        ExplicitDir explicitDir = new ExplicitDir(
                java.util.Map.of(),
                java.util.Set.of("excel_files"),
                java.util.Set.of()
        );

        // When: 使用显式目录配置创建 DirectoryStructure
        DirectoryStructure structure = new DirectoryStructure(tempDir, explicitDir);

        // Then: 应该只发现显式目录中的文件
        Collection<DirectoryStructure.ExcelFileInfo> excelFiles = structure.getExcelFiles();
        assertEquals(1, excelFiles.size(), "应该发现显式目录中的文件");

        DirectoryStructure.ExcelFileInfo excelFile = excelFiles.iterator().next();
        assertTrue(excelFile.relativePath().toString().endsWith("excel_files/user.csv") ||
                   excelFile.relativePath().toString().endsWith("excel_files\\user.csv"));
    }

    // --- 嵌套 JSON 目录测试 ---

    @Test
    void shouldDiscoverNestedJsonFilesWhenModuleDirContainsUnderscoreSubDir() throws IOException {
        // Given: 模块目录 buff 下有 _skill 子目录
        Path moduleDir = tempDir.resolve("buff");
        Path jsonDir = moduleDir.resolve("_skill");
        Files.createDirectories(jsonDir);

        String jsonData = "{\"id\": 1, \"name\": \"fireball\"}";
        Resources.addTempFileFromText("1.json", jsonDir, jsonData);

        // When: 创建 DirectoryStructure
        DirectoryStructure structure = new DirectoryStructure(tempDir);

        // Then: 应该通过嵌套路径发现 buff.skill 表的 JSON 文件
        Collection<DirectoryStructure.JsonFileInfo> jsonFiles = structure.getJsonFilesByTable("buff.skill");
        assertFalse(jsonFiles.isEmpty(), "应该通过嵌套路径发现 buff.skill 的 JSON 文件");
        assertEquals(1, jsonFiles.size());
    }

    @Test
    void shouldDiscoverNestedJsonFilesWhenModuleDirHasChineseSuffix() throws IOException {
        // Given: 模块目录名为 "skill_技能"（codeName 为 "skill"，中文在 _ 后面被截掉）
        Path moduleDir = tempDir.resolve("skill_技能");
        Path jsonDir = moduleDir.resolve("_buff");
        Files.createDirectories(jsonDir);

        String jsonData = "{\"id\": 1}";
        Resources.addTempFileFromText("1.json", jsonDir, jsonData);

        // When
        DirectoryStructure structure = new DirectoryStructure(tempDir);

        // Then: 应该发现 skill.buff 表
        Collection<DirectoryStructure.JsonFileInfo> jsonFiles = structure.getJsonFilesByTable("skill.buff");
        assertFalse(jsonFiles.isEmpty(), "应该通过 skill_技能 模块目录发现 skill.buff 的 JSON 文件");
    }

    @Test
    void shouldDiscoverDeeplyNestedJsonFiles() throws IOException {
        // Given: 多层嵌套 a/b/_c → 表名 a.b.c
        Path level1 = tempDir.resolve("a");
        Path level2 = level1.resolve("b");
        Path jsonDir = level2.resolve("_c");
        Files.createDirectories(jsonDir);

        String jsonData = "{\"id\": 1}";
        Resources.addTempFileFromText("1.json", jsonDir, jsonData);

        // When
        DirectoryStructure structure = new DirectoryStructure(tempDir);

        // Then
        Collection<DirectoryStructure.JsonFileInfo> jsonFiles = structure.getJsonFilesByTable("a.b.c");
        assertFalse(jsonFiles.isEmpty(), "应该发现多层嵌套 a.b.c 的 JSON 文件");
        assertEquals(1, jsonFiles.size());
    }

    @Test
    void shouldFallbackToRootLevelWhenModuleDirDoesNotExist() throws IOException {
        // Given: 没有模块目录，只有根级 _buff_skill 目录
        Path jsonDir = tempDir.resolve("_buff_skill");
        Files.createDirectories(jsonDir);

        String jsonData = "{\"id\": 1}";
        Resources.addTempFileFromText("1.json", jsonDir, jsonData);

        // When
        DirectoryStructure structure = new DirectoryStructure(tempDir);

        // Then: 应该通过旧格式发现 buff.skill 表
        Collection<DirectoryStructure.JsonFileInfo> jsonFiles = structure.getJsonFilesByTable("buff.skill");
        assertFalse(jsonFiles.isEmpty(), "应该通过旧格式根级目录发现 buff.skill 的 JSON 文件");
    }

    @Test
    void shouldThrowWhenBothNestedAndRootLevelExistForSameTable() throws IOException {
        // Given: 同时存在嵌套目录和根级目录，对应同一个表名
        Path moduleDir = tempDir.resolve("buff");
        Path nestedJsonDir = moduleDir.resolve("_skill");
        Files.createDirectories(nestedJsonDir);
        Resources.addTempFileFromText("1.json", nestedJsonDir, "{\"id\": 1}");

        Path rootJsonDir = tempDir.resolve("_buff_skill");
        Files.createDirectories(rootJsonDir);
        Resources.addTempFileFromText("2.json", rootJsonDir, "{\"id\": 2}");

        // When / Then: 应该抛出冲突异常
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            new DirectoryStructure(tempDir);
        });
        assertTrue(ex.getMessage().contains("conflict"), "应该报告冲突: " + ex.getMessage());
    }

    @Test
    void shouldDiscoverMultipleNestedTablesUnderSameModule() throws IOException {
        // Given: 同一模块下有多个嵌套 JSON 表目录
        Path moduleDir = tempDir.resolve("buff");
        Path jsonDir1 = moduleDir.resolve("_buff");
        Path jsonDir2 = moduleDir.resolve("_skill");
        Files.createDirectories(jsonDir1);
        Files.createDirectories(jsonDir2);

        Resources.addTempFileFromText("1.json", jsonDir1, "{\"id\": 1}");
        Resources.addTempFileFromText("2.json", jsonDir2, "{\"id\": 2}");

        // When
        DirectoryStructure structure = new DirectoryStructure(tempDir);

        // Then: 两个表都应该被发现
        assertFalse(structure.getJsonFilesByTable("buff.buff").isEmpty(), "应该发现 buff.buff");
        assertFalse(structure.getJsonFilesByTable("buff.skill").isEmpty(), "应该发现 buff.skill");
    }

    @Test
    void shouldNotTreatNonUnderscoreSubDirAsJsonTable() throws IOException {
        // Given: 模块目录下有非 _ 前缀的子目录
        Path moduleDir = tempDir.resolve("equip");
        Path normalSubDir = moduleDir.resolve("sub");
        Files.createDirectories(normalSubDir);

        // 创建 .json 文件（不应该被识别为 JSON 表数据）
        Resources.addTempFileFromText("1.json", normalSubDir, "{\"id\": 1}");

        // When
        DirectoryStructure structure = new DirectoryStructure(tempDir);

        // Then: 不应该发现任何 JSON 表文件
        assertTrue(structure.getJsonFilesByTable("equip.sub").isEmpty(),
                "非 _ 前缀子目录不应被识别为 JSON 表目录");
    }
}