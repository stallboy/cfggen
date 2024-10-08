package configgen.data;

import configgen.schema.CfgSchema;
import configgen.util.Logger;
import de.siegmar.fastcsv.reader.CsvRow;

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

import static configgen.data.CfgData.DRawRow;
import static configgen.data.CfgData.DRawSheet;
import static configgen.data.ExcelReader.*;


public class CfgDataReader {
    private final int headRow;
    private final ReadCsv csvReader;
    private final ExcelReader excelReader;

    public CfgDataReader(int headRow, ReadCsv csvReader, ExcelReader excelReader) {
        this.headRow = headRow;
        this.csvReader = csvReader;
        this.excelReader = excelReader;
        if (headRow < 2) {
            throw new IllegalArgumentException(String.format("headRow =%d < 2", headRow));
        }
    }

    record DRawCsvRow(CsvRow row) implements DRawRow {
        @Override
        public String cell(int c) {
            return c < row.getFieldCount() ? row.getField(c).trim() : "";
        }

        @Override
        public boolean isCellNumber(int c) {
            return false;
        }

        @Override
        public int count() {
            return row.getFieldCount();
        }
    }


    public CfgData readCfgData(Path rootDir, CfgSchema nullableCfgSchema) {
        try {
            return _readCfgData(rootDir, nullableCfgSchema);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CfgData _readCfgData(Path rootDir, CfgSchema nullableCfgSchema) throws Exception {
        DataStat stat = new DataStat();
        List<Callable<AllResult>> tasks = new ArrayList<>();

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
                            Logger.verbose2("%s 名字不符合规范，ignore！", path);
                            stat.ignoredCsvCount++;
                            return FileVisitResult.CONTINUE;
                        } else {
                            stat.csvCount++;
                            tasks.add(() -> csvReader.readCsv(path, relativePath, ti.tableName(), ti.index()));
                        }
                    }
                    case EXCEL -> {
                        tasks.add(() -> excelReader.readExcels(path, relativePath));
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
        List<Future<AllResult>> futures = executor.invokeAll(tasks);
        for (Future<AllResult> future : futures) {
            AllResult result = future.get();
            for (OneSheetResult sheet : result.sheets()) {
                addSheet(data, sheet.tableName(), sheet.sheet());
            }
            if (result.stat() != null) {
                stat.merge(result.stat());
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

    private void addSheet(CfgData cfgData, String tableName, DRawSheet sheetData) {
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
}