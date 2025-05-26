package configgen.data;

import configgen.ctx.DirectoryStructure;
import configgen.schema.CfgSchema;
import configgen.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static configgen.data.CfgData.DRawSheet;
import static configgen.data.ExcelReader.*;


public class CfgDataReader {
    private final int headRow;
    private final ReadCsv csvReader;
    private final ExcelReader excelReader;

    public CfgDataReader(int headRow, ReadCsv csvReader, ExcelReader excelReader) {
        Objects.requireNonNull(csvReader);
        Objects.requireNonNull(excelReader);
        this.headRow = headRow;
        this.csvReader = csvReader;
        this.excelReader = excelReader;
        if (headRow < 2) {
            throw new IllegalArgumentException(String.format("headRow =%d < 2", headRow));
        }
    }

    public CfgData readCfgData(DirectoryStructure sourceStructure, CfgSchema nullableCfgSchema) {
        Objects.requireNonNull(sourceStructure);
        try {
            return _readCfgData(sourceStructure, nullableCfgSchema);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CfgData _readCfgData(DirectoryStructure sourceStructure, CfgSchema nullableCfgSchema) throws Exception {
        CfgDataStat stat = new CfgDataStat();
        List<Callable<AllResult>> tasks = new ArrayList<>();

        for (DirectoryStructure.ExcelFileInfo df : sourceStructure.getExcelFiles()) {
            switch (df.fmt()) {
                case CSV -> {
                    DataUtil.TableNameIndex ti = df.csvTableNameIndex();
                    if (ti == null) {
                        Logger.verbose2("%s 名字不符合规范，ignore！", df.path());
                        stat.ignoredCsvCount++;
                    } else {
                        stat.csvCount++;
                        tasks.add(() -> csvReader.readCsv(df.path(), df.relativePath(),
                                ti.tableName(), ti.index()));
                    }
                }
                case EXCEL -> {
                    tasks.add(() -> excelReader.readExcels(df.path(), df.relativePath()));
                }
            }
        }

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
        List<Callable<CfgDataStat>> parseTasks = new ArrayList<>();
        for (CfgData.DTable table : data.tables().values()) {
            parseTasks.add(() -> {
                CfgDataStat tStat = new CfgDataStat();
                HeadParser.parse(table, tStat, nullableCfgSchema);
                CellParser.parse(table, tStat, nullableCfgSchema, headRow);
                return tStat;
            });
        }
        List<Future<CfgDataStat>> parseFutures = executor.invokeAll(parseTasks);
        for (Future<CfgDataStat> future : parseFutures) {
            CfgDataStat tStat = future.get();
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
