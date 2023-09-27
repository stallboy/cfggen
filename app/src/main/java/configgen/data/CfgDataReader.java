package configgen.data;

import configgen.Logger;
import configgen.util.UnicodeReader;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
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
import java.util.concurrent.*;

import static configgen.data.CfgData.*;
import static configgen.data.DataUtil.*;


public enum CfgDataReader {
    INSTANCE;

    public CfgData readCfgData(Path rootDir) {
        try {
            return _readCfgData(rootDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CfgData _readCfgData(Path rootDir) throws IOException, InterruptedException, ExecutionException {
        DataStat stat = new DataStat();
        List<Callable<Result>> tasks = new ArrayList<>();

        Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes a) {

                if (path.toFile().isHidden()) {
                    return FileVisitResult.CONTINUE;
                }

                if (path.getFileName().toString().startsWith("~")) {
                    return FileVisitResult.CONTINUE;
                }

                Path relativePath = rootDir.relativize(path);
                FileFmt fmt = getFileFormat(path);
                switch (fmt) {
                    case CSV -> {
                        stat.csvCount++;
                        TableNameIndex ti = getTableNameIndex(relativePath);
                        if (ti == null) {
                            Logger.verbose(STR. "\{ path } 名字不符合规范，ignore！" );
                            return FileVisitResult.CONTINUE;
                        } else {
                            tasks.add(() -> readCsvByFastCsv(path, ti));
                        }
                    }
                    case EXCEL -> {
                        tasks.add(() -> readExcelByFastExcel(path, relativePath));
                    }
                    case null -> {
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });


        CfgData cfgData = new CfgData(new TreeMap<>(), stat);
//        try(ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
//        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        try (ExecutorService executor = Executors.newWorkStealingPool()) {
            List<Future<Result>> futures = executor.invokeAll(tasks);
            for (Future<Result> future : futures) {
                Result result = future.get();

                if (result.isCsv) {
                    cfgData.addSheet(result.csvTableName, result.csvData);

                } else {
                    for (OneSheetResult sheet : result.excelData) {
                        cfgData.addSheet(sheet.tableName, sheet.sheetData);
                    }
                    stat.merge(result.excelStat);
                }
            }
        }

        return cfgData;
    }


    static class Result {
        boolean isCsv;
        String csvTableName;
        CsvData csvData;

        List<OneSheetResult> excelData;
        DataStat excelStat;
    }

    record OneSheetResult(String tableName,
                          ExcelSheetData sheetData) {
    }


    private Result readCsvByFastCsv(Path path, TableNameIndex ti) throws IOException {
        int count = 0;
        List<CsvRow> rows = new ArrayList<>();
        try (CsvReader reader = CsvReader.builder().build(new UnicodeReader(Files.newInputStream(path), "GBK"))) {
            for (CsvRow csvRow : reader) {
                if (count == 0) {
                    count = csvRow.getFieldCount();
                } else if (count != csvRow.getFieldCount()) {
                    Logger.verbose(STR. "\{ path } \{ csvRow.getOriginalLineNumber() } count \{ csvRow.getFieldCount() } not eq \{ count }" );
                }
                rows.add(csvRow);
            }
        }

        Result result = new Result();
        result.csvTableName = ti.tableName();
        result.isCsv = true;
        result.csvData = new CsvData(path.toAbsolutePath().normalize().toString(), ti.index(), rows);
        return result;
    }

    private Result readExcelByFastExcel(Path path, Path relativePath) throws IOException {
        DataStat stat = new DataStat();
        List<OneSheetResult> sheets = new ArrayList<>();

        stat.excelCount++;
        try (ReadableWorkbook wb = new ReadableWorkbook(path.toFile())) {
            for (Sheet sheet : wb.getSheets().toList()) {
                String sheetName = sheet.getName().trim();

                TableNameIndex ti = getTableNameIndex(relativePath, sheetName);
                if (ti == null) {
                    Logger.verbose(STR. "\{ path } [\{ sheetName }] 名字不符合规范，ignore！" );
                    continue;
                }

                stat.sheetCount++;
                List<Row> rows = sheet.read();
                OneSheetResult oneSheet = new OneSheetResult(ti.tableName(),
                        new ExcelSheetData(path.toAbsolutePath().normalize().toString(), sheetName, ti.index(), rows));
                sheets.add(oneSheet);

                int formula = 0;
                for (Row row : rows) {
                    for (Cell cell : row) {
                        if (cell != null) {
                            CellType type = cell.getType();
                            int old = stat.cellTypeCountMap.getOrDefault(type, 0);
                            stat.cellTypeCountMap.put(type, old + 1);
                            if (type == CellType.FORMULA) {
                                formula++;
//                                Logger.verbose(cell.getAddress() + ": " + cell.getFormula() + " " + cell);
                            }
                        } else {
                            stat.nullCellCount++;
                        }
                    }
                }

                if (formula > 0) {
                    Logger.verbose(STR. "\{ path } [\{ sheetName }] formula count=\{ formula }" );
                }
            }
        }
        Result result = new Result();
        result.isCsv = false;
        result.excelData = sheets;
        result.excelStat = stat;
        return result;
    }

    public static void main(String[] args) {
        Logger.enableVerbose();
        Logger.mm("start readCfgData");
        CfgData cfgData = CfgDataReader.INSTANCE.readCfgData(Path.of("."));
        Logger.mm("end readCfgData");

        cfgData.stat().print();
        System.out.println("table\t" + cfgData.tables().size());
    }
}


