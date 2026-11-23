package configgen.data;

import org.jspecify.annotations.Nullable;

import configgen.data.DataUtil.TableNameIndex;
import configgen.schema.CfgSchema;
import configgen.schema.CfgSchemaErrs;
import configgen.schema.SchemaUtil;
import configgen.util.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static configgen.data.CfgData.DRawSheet;
import static configgen.data.DataUtil.getTableNameIndex;


public record CfgDataReader(HeadRow headRow,
                            ReadCsv csvReader,
                            ExcelReader excelReader) {

    public CfgDataReader {
        Objects.requireNonNull(headRow);
        Objects.requireNonNull(csvReader);
        Objects.requireNonNull(excelReader);
    }

    public CfgData readCfgData(Collection<ExcelFileInfo> excelFiles, @Nullable CfgSchema nullableCfgSchema, CfgSchemaErrs errs) {
        Objects.requireNonNull(excelFiles);
        Objects.requireNonNull(errs);
        try {
            return _readCfgData(excelFiles, nullableCfgSchema, errs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CfgData _readCfgData(Collection<ExcelFileInfo> excelFiles, @Nullable CfgSchema nullableCfgSchema, CfgSchemaErrs errs)
            throws Exception {
//        try(ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
//        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        try (ExecutorService executor = Executors.newWorkStealingPool()) {
            CfgDataStat stat = new CfgDataStat();
            CfgData data = new CfgData(new TreeMap<>(), stat);

            // read all csv/excel files
            List<Callable<ReadResult>> tasks = new ArrayList<>();
            for (ExcelFileInfo df : excelFiles) {
                switch (df.fmt()) {
                    case CSV -> {
                        TableNameIndex ti = getTableNameIndex(df.relativePath());
                        if (ti == null) {
                            Logger.verbose2("%s 名字不符合规范，ignore！", df.path());
                            stat.ignoredCsvCount++;
                        } else {
                            stat.csvCount++;
                            tasks.add(() -> {
                                try {
                                    return csvReader.readCsv(
                                            df.path(), df.relativePath(),
                                            ti.tableName(), ti.index());
                                } catch (Exception e) {
                                    throw new RuntimeException("read csv failed: " + df.path(), e);
                                }
                            });
                        }
                    }
                    case EXCEL -> {
                        tasks.add(() -> {
                            try {
                                return excelReader.readExcels(df.path(), df.relativePath(), null);
                            } catch (Exception e) {
                                throw new RuntimeException("read excel failed: " + df.path(), e);
                            }
                        });
                    }
                }
            }
            List<Future<ReadResult>> futures = executor.invokeAll(tasks);
            for (Future<ReadResult> future : futures) {
                ReadResult result = future.get();
                for (ReadResult.OneSheet sheet : result.sheets()) {
                    addSheet(data, sheet.tableName(), sheet.sheet());
                }
                stat.merge(result.stat());
            }
            Logger.profile("data read");

            // parse head & data cell
            // errs 非线程安全（内部是ArrayList），并行任务各自收集，主线程merge
            List<Callable<ParseResult>> parseTasks = new ArrayList<>(data.tables().size());
            for (CfgData.DTable table : data.tables().values()) {
                parseTasks.add(() -> {
                    CfgDataStat tStat = new CfgDataStat();
                    CfgSchemaErrs tErrs = CfgSchemaErrs.of();
                    boolean isColumnMode = SchemaUtil.isColumnMode(nullableCfgSchema, table.tableName());
                    try {
                        HeadParser.parse(table, tStat, headRow, isColumnMode, tErrs);
                        CellParser.parse(table, tStat, headRow.rowCount(), isColumnMode);
                    } catch (Exception e) {
                        throw new RuntimeException("parse table failed: " + table.tableName(), e);
                    }
                    return new ParseResult(tStat, tErrs);
                });
            }
            List<Future<ParseResult>> parseFutures = executor.invokeAll(parseTasks);
            for (Future<ParseResult> future : parseFutures) {
                ParseResult pr = future.get();
                stat.merge(pr.stat());
                errs.merge(pr.errs());
            }
            Logger.profile("data parse");
            stat.tableCount = data.tables().size();
            return data;
        }
    }


    private void addSheet(CfgData cfgData, String tableName, DRawSheet sheetData) {
        CfgData.DTable dTable = cfgData.getDTable(tableName);
        if (dTable != null) {
            dTable.rawSheets().add(sheetData);
        } else {
            List<DRawSheet> sheets = new ArrayList<>();
            sheets.add(sheetData);
            CfgData.DTable newTable = CfgData.DTable.of(tableName, sheets);
            cfgData.tables().put(tableName, newTable);
        }
    }

    private record ParseResult(CfgDataStat stat, CfgSchemaErrs errs) {
    }
}
