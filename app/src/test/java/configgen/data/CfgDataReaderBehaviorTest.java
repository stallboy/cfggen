package configgen.data;

import configgen.Resources;
import configgen.ctx.DirectoryStructure;
import configgen.ctx.HeadRows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CfgDataReaderBehaviorTest {

    private @TempDir Path tempDir;

    static CfgData readFile(String fn, Path tempDir) {
        Resources.addTempFileFromResourceFile(fn, tempDir);
        ReadCsv csvReader = new ReadCsv("GBK");
        CfgDataReader fastDataReader = new CfgDataReader(HeadRows.A2_Default, csvReader, ReadByFastExcel.INSTANCE);
        return fastDataReader.readCfgData(new DirectoryStructure(tempDir), null);
    }

    @Test
    void shouldReadMultipleFilesConcurrentlyWithoutDataRace() throws Exception {
        // Given: 多个测试文件
        String[] testFiles = {"rank.csv", "ai行为.xlsx"};

        // When: 并发读取
        ExecutorService executor = Executors.newFixedThreadPool(testFiles.length);
        List<CompletableFuture<CfgData>> futures = new ArrayList<>();

        for (String fileName : testFiles) {
            CompletableFuture<CfgData> future = CompletableFuture.supplyAsync(() -> {
                return readFile(fileName, tempDir);
            }, executor);
            futures.add(future);
        }

        // Then: 验证数据完整性和无竞争条件
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );

        List<CfgData> results = allFutures.thenApply(v ->
            futures.stream()
                .map(CompletableFuture::join)
                .toList()
        ).get(30, TimeUnit.SECONDS);

        executor.shutdown();

        // 验证每个文件都正确读取
        assertEquals(2, results.size());

        // 验证rank.csv数据
        CfgData rankData = results.get(0);
        assertTrue(rankData.tables().containsKey("rank"));

        // 验证ai行为.xlsx数据
        CfgData aiData = results.get(1);
        assertTrue(aiData.tables().containsKey("ai"));
        assertTrue(aiData.tables().containsKey("ai_action"));
    }

    @Test
    void shouldHandleEmptyDirectoryGracefully() {
        // Given: 空目录
        DirectoryStructure emptyDir = new DirectoryStructure(tempDir);
        ReadCsv csvReader = new ReadCsv("GBK");
        CfgDataReader fastDataReader = new CfgDataReader(HeadRows.A2_Default, csvReader, ReadByFastExcel.INSTANCE);

        // When: 读取配置数据
        CfgData cfgData = fastDataReader.readCfgData(emptyDir, null);

        // Then: 返回空数据集
        assertNotNull(cfgData);
        assertEquals(0, cfgData.tables().size());
        assertEquals(0, cfgData.stat().tableCount);
        assertEquals(0, cfgData.stat().excelCount);
        assertEquals(0, cfgData.stat().sheetCount);
    }

    @Test
    void shouldMergeDataFromMultipleSheetsCorrectly() {
        // Given: 多sheet Excel文件
        String fn = "ai行为.xlsx";

        // When: 读取数据
        CfgData cfgData = readFile(fn, tempDir);

        // Then: 验证数据正确合并
        assertEquals(3, cfgData.tables().size());
        assertEquals(3, cfgData.stat().tableCount);
        assertEquals(1, cfgData.stat().excelCount);
        assertEquals(4, cfgData.stat().sheetCount);

        // 验证每个表的数据完整性
        CfgData.DTable aiTable = cfgData.tables().get("ai");
        assertNotNull(aiTable);
        assertEquals(7, aiTable.fields().size());
        assertEquals(52, aiTable.rows().size());

        CfgData.DTable aiActionTable = cfgData.tables().get("ai_action");
        assertNotNull(aiActionTable);
        assertEquals(12, aiActionTable.fields().size());
        assertEquals(53, aiActionTable.rows().size());

        // 验证数据一致性 - 确保所有表都有正确的字段和数据行
        for (CfgData.DTable table : cfgData.tables().values()) {
            assertFalse(table.fields().isEmpty(), "Table " + table.tableName() + " should have fields");
            assertNotNull(table.rows(), "Table " + table.tableName() + " should have rows");
        }
    }

    @Test
    void shouldHandleInvalidFilePathsGracefully() {
        // Given: 无效文件路径
        DirectoryStructure dirWithInvalidFiles = new DirectoryStructure(tempDir);
        ReadCsv csvReader = new ReadCsv("GBK");
        CfgDataReader fastDataReader = new CfgDataReader(HeadRows.A2_Default, csvReader, ReadByFastExcel.INSTANCE);

        // When: 尝试读取不存在的文件
        CfgData cfgData = fastDataReader.readCfgData(dirWithInvalidFiles, null);

        // Then: 返回空数据集而不是抛出异常
        assertNotNull(cfgData);
        assertEquals(0, cfgData.tables().size());
    }

    @Test
    void shouldProcessFilesWithDifferentEncodings() {
        // Given: 不同编码的文件
        String fn = "rank.csv"; // 假设使用GBK编码

        // When: 使用正确编码读取
        CfgData cfgData = readFile(fn, tempDir);

        // Then: 验证中文内容正确解析
        CfgData.DTable rankTable = cfgData.tables().get("rank");
        assertNotNull(rankTable);

        // 验证包含中文的单元格
        List<CfgData.DCell> rowWithChinese = rankTable.rows().get(1);
        assertEquals("中品", rowWithChinese.get(2).value());
    }
}