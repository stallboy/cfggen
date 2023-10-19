package configgen.data;

import configgen.schema.CfgSchema;
import configgen.util.Logger;
import configgen.util.UnicodeReader;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dhatim.fastexcel.reader.*;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static configgen.data.CfgData.*;


public enum CfgDataReader {
    INSTANCE;

    public CfgData readCfgData(Path rootDir, CfgSchema nullableCfgSchema, int headRow, boolean checkComma, String defaultEncoding) {
        if (headRow < 2) {
            throw new IllegalArgumentException(STR. "headRow =\{ headRow } < 2" );
        }
        try {
            return _readCfgData(rootDir, nullableCfgSchema, headRow, checkComma, defaultEncoding);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CfgData _readCfgData(Path rootDir, CfgSchema nullableCfgSchema,
                                 int headRow, boolean checkComma, String defaultEncoding) throws Exception {
        DataStat stat = new DataStat();
        List<Callable<Result>> tasks = new ArrayList<>();

        Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes a) {

                if (filePath.toFile().isHidden()) {
                    return FileVisitResult.CONTINUE;
                }

                if (filePath.getFileName().toString().startsWith("~")) {
                    return FileVisitResult.CONTINUE;
                }

                Path relativePath = rootDir.relativize(filePath);
                Path path = filePath.toAbsolutePath().normalize();
                DataUtil.FileFmt fmt = DataUtil.getFileFormat(path);
                switch (fmt) {
                    case CSV -> {
                        DataUtil.TableNameIndex ti = DataUtil.getTableNameIndex(relativePath);
                        if (ti == null) {
                            Logger.verbose2(STR. "\{ path } 名字不符合规范，ignore！" );
                            stat.ignoredCsvCount++;
                            return FileVisitResult.CONTINUE;
                        } else {
                            stat.csvCount++;
                            tasks.add(() -> readCsvByFastCsv(path, relativePath, ti, defaultEncoding));
                        }
                    }
                    case EXCEL -> {
                        if (checkComma) {
                            tasks.add(() -> readExcelByPoi(path, relativePath));
                        } else {
                            tasks.add(() -> readExcelByFastExcel(path, relativePath));
                        }
                    }
                    case null -> {
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });


        CfgData data = new CfgData(new TreeMap<>(), stat);
//        try(ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
//        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        ExecutorService executor = Executors.newWorkStealingPool();
        List<Future<Result>> futures = executor.invokeAll(tasks);
        for (Future<Result> future : futures) {
            Result result = future.get();
            for (OneSheetResult sheet : result.sheets) {
                addSheet(data, sheet.tableName, sheet.sheet);
            }
            if (result.stat != null) {
                stat.merge(result.stat);
            }
        }

        Logger.profile("data read");
        List<Callable<DataStat>> parseTasks = new ArrayList<>();
        for (CfgData.DTable table : data.tables().values()) {
            parseTasks.add(() -> {
                DataStat tStat = new DataStat();
                HeadParser.parse(table, tStat, nullableCfgSchema);
                CellParser.parse(table, tStat, nullableCfgSchema, headRow);
                return tStat;
            });
        }
        List<Future<DataStat>> parseFutures = executor.invokeAll(parseTasks);
        for (Future<DataStat> future : parseFutures) {
            DataStat tStat = future.get();
            stat.merge(tStat);
        }
        Logger.profile("data parse");

        stat.tableCount = data.tables().size();
        executor.close();
        return data;
    }

    void addSheet(CfgData cfgData, String tableName, DRawSheet sheetData) {
        CfgData.DTable DTable = cfgData.tables().get(tableName);
        if (DTable != null) {
            DTable.rawSheets().add(sheetData);
        } else {
            List<DRawSheet> sheets = new ArrayList<>();
            sheets.add(sheetData);
            CfgData.DTable newTable = new CfgData.DTable(tableName, new ArrayList<>(), new ArrayList<>(), sheets);
            cfgData.tables().put(tableName, newTable);
        }
    }


    record Result(List<OneSheetResult> sheets,
                  DataStat stat) {
    }

    record OneSheetResult(String tableName,
                          DRawSheet sheet) {
    }


    private Result readCsvByFastCsv(Path path, Path relativePath, DataUtil.TableNameIndex ti,
                                    String defaultEncoding) throws IOException {
        int count = 0;
        DataStat stat = new DataStat();
        List<DRawRow> rows = new ArrayList<>();
        try (CsvReader reader = CsvReader.builder().build(new UnicodeReader(Files.newInputStream(path), defaultEncoding))) {
            for (CsvRow csvRow : reader) {
                stat.cellCsvCount += csvRow.getFieldCount();
                if (count == 0) {
                    count = csvRow.getFieldCount();
                } else if (count != csvRow.getFieldCount()) {
                    Logger.verbose2(STR. "\{ path } \{ csvRow.getOriginalLineNumber() } field count \{ csvRow.getFieldCount() } not eq \{ count }" );
                }
                rows.add(new DRawCsvRow(csvRow));
            }
        }
        DRawSheet sheet = new DRawSheet(relativePath.toString(), "", ti.index(), rows, new ArrayList<>());
        return new Result(List.of(new OneSheetResult(ti.tableName(), sheet)), stat);
    }

    private Result readExcelByFastExcel(Path path, Path relativePath) throws IOException {
        DataStat stat = new DataStat();
        List<OneSheetResult> sheets = new ArrayList<>();

        stat.excelCount++;
        try (ReadableWorkbook wb = new ReadableWorkbook(path.toFile(), new ReadingOptions(true, false))) {
            for (Sheet sheet : wb.getSheets().toList()) {
                String sheetName = sheet.getName().trim();

                DataUtil.TableNameIndex ti = DataUtil.getTableNameIndex(relativePath, sheetName);
                if (ti == null) {
                    Logger.verbose2(STR. "\{ path } [\{ sheetName }] 名字不符合规范，ignore！" );
                    stat.ignoredSheetCount++;
                    continue;
                }

                stat.sheetCount++;
                List<Row> rawRows = sheet.read();
                List<DRawRow> rows = new ArrayList<>(rawRows.size());
                for (Row cells : rawRows) {
                    rows.add(new DRawExcelRow(cells));
                }
                OneSheetResult oneSheet = new OneSheetResult(ti.tableName(),
                        new DRawSheet(relativePath.toString(), sheetName, ti.index(), rows, new ArrayList<>()));
                sheets.add(oneSheet);

                int formula = 0;
                for (Row row : rawRows) {
                    for (Cell cell : row) {
                        if (cell != null) {
                            CellType type = cell.getType();
                            switch (type) {
                                case NUMBER -> {
                                    stat.cellNumberCount++;
                                }
                                case STRING -> {
                                    stat.cellStrCount++;
                                }
                                case FORMULA -> {
                                    stat.cellFormulaCount++;
                                }
                                case ERROR -> {
                                    stat.cellErrCount++;
                                }
                                case BOOLEAN -> {
                                    stat.cellBoolCount++;
                                }
                                case EMPTY -> {
                                    stat.cellEmptyCount++;
                                }
                            }

                            if (type == CellType.FORMULA) {
                                formula++;
                                // Logger.verbose2(cell.getAddress() + ": formula=" + cell.getFormula() + ", text=" + cell.getText());
                            }

                        } else {
                            stat.cellNullCount++;
                        }
                    }
                }

                if (formula > 0) {
                    Logger.verbose2(STR. "\{ path } [\{ sheetName }] formula count=\{ formula }" );
                }
            }
        }
        return new Result(sheets, stat);
    }

    private Result readExcelByPoi(Path path, Path relativePath) throws IOException {
        DataStat stat = new DataStat();
        List<OneSheetResult> sheets = new ArrayList<>();

        stat.excelCount++;


        try (Workbook workbook = WorkbookFactory.create(path.toFile(), null, true)) {
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DRawPoiFmt fmt = new DRawPoiFmt(formatter, evaluator);
            for (org.apache.poi.ss.usermodel.Sheet sheet : workbook) {
                String sheetName = sheet.getSheetName().trim();
                DataUtil.TableNameIndex ti = DataUtil.getTableNameIndex(relativePath, sheetName);
                if (ti == null) {
                    Logger.verbose2(STR. "\{ path } [\{ sheetName }] 名字不符合规范，ignore！" );
                    stat.ignoredSheetCount++;
                    continue;
                }

                stat.sheetCount++;
                List<DRawRow> rows = new ArrayList<>(sheet.getLastRowNum() + 1);
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    rows.add(new DRawPoiExcelRow(row, fmt));
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        switch (cell.getCellType()) {
                            case NUMERIC -> {
                                stat.cellNumberCount++;
                            }
                            case STRING -> {
                                stat.cellStrCount++;
                            }
                            case FORMULA -> {
                                stat.cellFormulaCount++;
                            }
                            case BLANK -> {
                                stat.cellEmptyCount++;
                            }
                            case BOOLEAN -> {
                                stat.cellBoolCount++;
                            }
                            case ERROR -> {
                                stat.cellErrCount++;
                            }
                        }
                    }
                }
                OneSheetResult oneSheet = new OneSheetResult(ti.tableName(),
                        new DRawSheet(relativePath.toString(), sheetName, ti.index(), rows, new ArrayList<>()));
                sheets.add(oneSheet);
            }
        }

        return new Result(sheets, stat);
    }
}


