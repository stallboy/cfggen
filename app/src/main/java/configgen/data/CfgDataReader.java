package configgen.data;

import configgen.ctx.DirectoryStructure;
import configgen.ctx.HeadRow;
import configgen.data.DataUtil.TableNameIndex;
import configgen.schema.CfgSchema;
import configgen.schema.CfgSchemaErrs;
import configgen.schema.SchemaUtil;
import configgen.schema.TableSchema;
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
import static configgen.data.DataUtil.getTableNameIndex;


public record CfgDataReader(HeadRow headRow,
                            ReadCsv csvReader,
                            ExcelReader excelReader) {

    public CfgDataReader {
        Objects.requireNonNull(headRow);
        Objects.requireNonNull(csvReader);
        Objects.requireNonNull(excelReader);
    }

    public CfgData readCfgData(DirectoryStructure sourceStructure, CfgSchema nullableCfgSchema, CfgSchemaErrs errs) {
        Objects.requireNonNull(sourceStructure);
        Objects.requireNonNull(errs);
        try {
            return _readCfgData(sourceStructure, nullableCfgSchema, errs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("ExtractMethodRecommender")
    private CfgData _readCfgData(DirectoryStructure sourceStructure, CfgSchema nullableCfgSchema, CfgSchemaErrs errs) throws Exception {
//        try(ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
//        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        try (ExecutorService executor = Executors.newWorkStealingPool()) {
            CfgDataStat stat = new CfgDataStat();
            CfgData data = new CfgData(new TreeMap<>(), stat);

            // read all csv/excel files
            List<Callable<ReadResult>> tasks = new ArrayList<>();
            for (DirectoryStructure.ExcelFileInfo df : sourceStructure.getExcelFiles()) {
                switch (df.fmt()) {
                    case CSV, TXT_AS_TSV -> {
                        TableNameIndex ti = getTableNameIndex(df.relativePath());
                        if (ti == null) {
                            Logger.verbose2("%s 名字不符合规范，ignore！", df.path());
                            stat.ignoredCsvCount++;
                        } else {
                            stat.csvCount++;
                            char fieldSeparator = df.fmt() == DataUtil.FileFmt.CSV ? ',' : '\t';
                            tasks.add(() -> csvReader.readCsv(df.path(), df.relativePath(),
                                    ti.tableName(), ti.index(), fieldSeparator, df.nullableAddTag()));
                        }
                    }
                    case EXCEL -> {
                        tasks.add(() -> excelReader.readExcels(df.path(), df.relativePath(), null));
                    }
                }
            }
            List<Future<ReadResult>> futures = executor.invokeAll(tasks);
            for (Future<ReadResult> future : futures) {
                ReadResult result = future.get();
                for (ReadResult.OneSheet sheet : result.sheets()) {
                    addSheet(data, sheet.tableName(), sheet.sheet(), result.nullableAddTag());
                }
                stat.merge(result.stat());
            }
            Logger.profile("data read");

            // parse head & data cell
            List<Callable<CfgDataStat>> parseTasks = new ArrayList<>(data.tables().size());
            for (CfgData.DTable table : data.tables().values()) {
                parseTasks.add(() -> {
                    CfgDataStat tStat = new CfgDataStat();
                    boolean isColumnMode = SchemaUtil.isColumnMode(nullableCfgSchema, table.tableName());
                    HeadParser.parse(table, tStat, headRow, isColumnMode, errs);
                    CellParser.parse(table, tStat, headRow.rowCount(), isColumnMode);
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
            return data;
        }
    }



    private void addSheet(CfgData cfgData, String tableName, DRawSheet sheetData, String nullableAddTag) {
        CfgData.DTable dTable = cfgData.getDTable(tableName);
        if (dTable != null) {
            dTable.rawSheets().add(sheetData);
        } else {
            List<DRawSheet> sheets = new ArrayList<>();
            sheets.add(sheetData);
            CfgData.DTable newTable = CfgData.DTable.of(tableName, sheets, nullableAddTag);
            cfgData.tables().put(tableName, newTable);
        }
    }
}
