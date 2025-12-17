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
}