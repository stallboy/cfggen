package configgen.data;

import configgen.gen.BuildSettings;
import configgen.schema.CfgSchema;
import configgen.util.Logger;
import configgen.util.UnicodeReader;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;

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

import static configgen.data.CfgData.DRawRow;
import static configgen.data.CfgData.DRawSheet;
import static configgen.data.ExcelReader.*;


public enum CfgDataReader {
    INSTANCE;

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


    public CfgData readCfgData(Path rootDir, CfgSchema nullableCfgSchema, int headRow, boolean usePoi, String defaultEncoding) {
        if (headRow < 2) {
            throw new IllegalArgumentException(STR. "headRow =\{ headRow } < 2" );
        }
        try {
            return _readCfgData(rootDir, nullableCfgSchema, headRow, usePoi, defaultEncoding);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CfgData _readCfgData(Path rootDir, CfgSchema nullableCfgSchema,
                                 int headRow, boolean usePoi, String defaultEncoding) throws Exception {
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
                            Logger.verbose2(STR. "\{ path } 名字不符合规范，ignore！" );
                            stat.ignoredCsvCount++;
                            return FileVisitResult.CONTINUE;
                        } else {
                            stat.csvCount++;
                            tasks.add(() -> readCsvByFastCsv(path, relativePath, ti, defaultEncoding));
                        }
                    }
                    case EXCEL -> {
                        if (BuildSettings.is_include_poi && usePoi) {
                            tasks.add(() -> BuildSettings.poiReader.readExcels(path, relativePath));
                        } else {
                            tasks.add(() -> ReadByFastExcel.INSTANCE.readExcels(path, relativePath));
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


    private AllResult readCsvByFastCsv(Path path, Path relativePath, DataUtil.TableNameIndex ti,
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
        return new AllResult(List.of(new OneSheetResult(ti.tableName(), sheet)), stat);
    }

}


