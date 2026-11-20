package configgen.write;

import com.alibaba.fastjson2.JSONObject;
import configgen.Resources;
import configgen.ctx.Context;
import configgen.data.CfgData.DTable;
import configgen.util.Logger;
import configgen.util.CSVUtil;
import configgen.value.CfgValue;
import configgen.value.ValueUtil;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static configgen.value.CfgValue.VList;
import static configgen.value.CfgValue.VStruct;
import static configgen.value.CfgValue.VTable;
import static configgen.value.CfgValue.Value;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 addOrUpdateRecord 对嵌套 block 表的幂等性：同一份 record 反复写回 + reload，
 * wave/spawn 数不漂移、文件无残留行。
 *
 * <p>复现 levelmonster 的"点更新多出节点"：根因是清空行数算少了（VTableStorage.findRecordLoc
 * 用 mapToBlock 的逻辑行数，不含人工空分隔行），残留的旧行在 reload 时被当成新元素。
 */
class VTableStorageTest {
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
    void deletingRecordPreservesUnmappedColumn() throws IOException {
        Resources.addTempFileFromText("config.cfg", tempDir, "table t[id] { id:int; }");
        Path source = tempDir.resolve("t.csv");
        Files.writeString(source, "ignored,ID\n,id\ncomment,1\nkeep,2\n");
        Context context = new Context(tempDir);

        var result = DeleteService.deleteRecord(context, context.makeValue(), "t", "1");

        assertEquals(DeleteService.DeleteErrorCode.OK, result.errorCode());
        assertEquals(List.of("comment", ""), CSVUtil.read(source).get(2).getFields());
        assertEquals(List.of("keep", "2"), CSVUtil.read(source).get(3).getFields());
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void deletingExcelBlockPreservesUnmappedCellsOnEveryLine(boolean xlsx, boolean columnMode) throws IOException {
        String metadata = columnMode ? " (columnMode)" : "";
        Resources.addTempFileFromText("config.cfg", tempDir,
                "table t[id]" + metadata + " { id:int; values:list<int> (block=1); }");
        Path source = tempDir.resolve(xlsx ? "t.xlsx" : "t.xls");
        String[][] data = {
                {"ID", "Notes", "Values"},
                {"id", "", "values"},
                {"1", "first", "10"},
                {"", "continuation", "11"},
                {"2", "next", "20"}
        };
        try (var workbook = WorkbookFactory.create(xlsx);
             var output = Files.newOutputStream(source)) {
            var sheet = workbook.createSheet("t");
            for (int line = 0; line < data.length; line++) {
                for (int field = 0; field < data[line].length; field++) {
                    int row = columnMode ? field : line;
                    int col = columnMode ? line : field;
                    var physicalRow = sheet.getRow(row);
                    if (physicalRow == null) {
                        physicalRow = sheet.createRow(row);
                    }
                    physicalRow.createCell(col).setCellValue(data[line][field]);
                }
            }
            workbook.write(output);
        }

        if (xlsx) {
            Context context = new Context(tempDir);
            var result = DeleteService.deleteRecord(context, context.makeValue(), "t", "1");
            assertEquals(DeleteService.DeleteErrorCode.OK, result.errorCode());
            assertEquals(1, requireNonNull(new Context(tempDir).makeValue().getTable("t")).primaryKeyMap().size());
        } else {
            // Context's default FastExcel reader only supports OOXML.
            try (TableFile file = columnMode
                    ? new ColumnModeExcelTableFile(source, "t", 2)
                    : new ExcelTableFile(source, "t", 2)) {
                file.emptyRows(2, 2, List.of(0, 2));
                file.saveAndClose();
            }
        }

        try (var workbook = WorkbookFactory.create(source.toFile(), null, true)) {
            Sheet sheet = workbook.getSheet("t");
            for (int line : List.of(2, 3)) {
                assertEquals(line == 2 ? "first" : "continuation",
                        sheet.getRow(columnMode ? 1 : line).getCell(columnMode ? line : 1).getStringCellValue());
                assertEquals(CellType.BLANK,
                        sheet.getRow(columnMode ? 2 : line).getCell(columnMode ? line : 2).getCellType());
            }
            assertEquals("20", sheet.getRow(columnMode ? 2 : 4).getCell(columnMode ? 4 : 2).getStringCellValue());
        }
    }

    @Test
    void failedExcelEditClosesWorkbookWithoutChangingFile() throws IOException {
        Path source = tempDir.resolve("t.xlsx");
        try (var workbook = new XSSFWorkbook();
             var output = Files.newOutputStream(source)) {
            workbook.createSheet("t").createRow(2).createCell(0).setCellValue("old");
            workbook.write(output);
        }
        byte[] original = Files.readAllBytes(source);

        var file = new ExcelTableFile(source, "t", 2);
        var workbookPackage = ((XSSFWorkbook) file.workbook).getPackage();
        assertThrows(IllegalArgumentException.class, () -> {
            try (file) {
                file.emptyRows(2, 1, null);
                RecordBlock block = new RecordBlock(1);
                block.setCell(0, 0, "x".repeat(32768));
                file.insertRecordBlock(2, 1, new RecordBlock.RecordBlockTransformed(block, List.of(0)));
            }
        });
        assertTrue(workbookPackage.isClosed());
        assertArrayEquals(original, Files.readAllBytes(source));
    }

    @Test
    void sepTextListRoundTripsThroughCsvWriteAndReload() throws IOException {
        Resources.addTempFileFromText("config.cfg", tempDir,
                "table t[id] { id:int; values:list<str> (sep=';'); }");
        Path source = tempDir.resolve("t.csv");
        CSVUtil.writeToFile(source.toFile(), List.of(List.of("ID", "Values"),
                List.of("id", "values"), List.of("1", ";legacy;;")));
        Context context = new Context(tempDir);
        CfgValue initial = context.makeValue();
        assertEquals(List.of("legacy"), firstStringList(initial));

        List<String> expected = List.of("", "a;b", "a\"b", "a\rb", "");
        String json = JSONObject.of("id", 1, "values", expected).toJSONString();
        var result = AddOrUpdateService.addOrUpdateRecord(context, initial, "t", json);

        assertEquals(AddOrUpdateService.AddOrUpdateErrorCode.UpdateOK, result.errorCode());
        assertEquals(expected,
                firstStringList(new Context(tempDir).makeValue()));
    }

    private static List<String> firstStringList(CfgValue value) {
        var record = requireNonNull(value.getTable("t")).primaryKeyMap().values().iterator().next();
        return ((VList) record.values().get(1)).valueList().stream()
                .map(item -> ((CfgValue.VString) item).value()).toList();
    }

    @Test
    void addOrUpdateRecord_idempotentWithNestedBlockAndManualSeparator() throws IOException {
        String cfgStr = """
                struct LevelWave {
                    waveIndex:int;
                    startTime:float;
                    spawns:list<LevelSpawn> (block=1);
                }
                struct LevelSpawn {
                    pathId:int;
                    monsterId:int;
                }
                table t[id] {
                    id:int;
                    waves:list<LevelWave> (block=1);
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);

        // 列: id, waveIndex, startTime, 〔空分隔列〕, pathId, monsterId
        // 关键: 两个 wave 之间放一行全空人工分隔行（模拟 levelmonster 的视觉分组空白，
        // 它会被 CellParser 从内存表过滤，但物理行仍在文件里，使物理行跨度 > 逻辑元素行数）
        String csvStr = """
                关卡ID,波次,启动时间,总时间,路径,怪物ID
                id,waves,startTime,,pathId,monsterId
                1,1,0.0,1,1001,2001
                ,,1.0,1,1002,2002
                ,,1.0,1,1003,2003
                ,,,,,
                ,2,2.0,1,1004,2004
                ,,2.0,1,1005,2005
                """;
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        VTable vTable = requireNonNull(cfgValue.getTable("t"));
        DTable dTable = requireNonNull(ctx.cfgData().getDTable("t"));

        int expectedWaves = 2;
        int[] expectedSpawns = {3, 2};

        VStruct record = vTable.primaryKeyMap().values().iterator().next();
        Value pk = ValueUtil.extractPrimaryKeyValue(record, vTable.schema());

        for (int round = 0; round < 3; round++) {
            // 把当前 record 原样写回（模拟“没改数据点更新”）
            VTableStorage.addOrUpdateRecord(ctx, vTable, dTable, pk, record);

            ValueUpdater.NewCfgValueResult nr = ValueUpdater.updateByReloadTableData(ctx, cfgValue, vTable);
            ctx.updateDataAndValue(nr.newCfgData(), nr.newCfgValue());
            cfgValue = nr.newCfgValue();
            vTable = requireNonNull(cfgValue.getTable("t"));
            dTable = requireNonNull(ctx.cfgData().getDTable("t"));
            record = requireNonNull(vTable.primaryKeyMap().get(pk));

            VList waves = (VList) record.values().get(1);
            assertEquals(expectedWaves, waves.valueList().size(),
                    "round " + round + " wave count drift");
            for (int i = 0; i < expectedWaves; i++) {
                VList spawns = (VList) ((VStruct) waves.valueList().get(i)).values().get(2);
                assertEquals(expectedSpawns[i], spawns.valueList().size(),
                        "round " + round + " wave " + (i + 1) + " spawn count drift");
            }
        }

        // 文件物理行无残留：block=1 下每个 spawn 占一行（wave header 行含 spawn1），
        // 故非空数据行数应 == spawn 总数；若清空行数算少了，残留旧行会使该数变大。
        var rows = CSVUtil.read(tempDir.resolve("t.csv"));
        long nonEmptyDataLines = rows.stream()
                .skip(2)
                .filter(row -> List.of(0, 1, 2, 4, 5).stream().anyMatch(i -> !row.getField(i).isEmpty()))
                .count();
        assertEquals(5, rows.stream().skip(2).filter(row -> row.getField(3).equals("1")).count());
        long expected = Arrays.stream(expectedSpawns).sum();
        assertEquals(expected, nonEmptyDataLines, "physical data line count (residue check)");
    }
}
