package configgen.value;

import configgen.schema.EntryType;
import configgen.schema.TableSchema;
import configgen.schema.TableSchemaRefGraph;
import configgen.util.LocaleUtil;
import configgen.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.*;

public class UnreferencedRecordCollector {

    public record Unreferenced(int total,
                               Map<String, List<UnreferencedRecord>> tableToUnreferenced) {
        public void print() {
            if (total > 0) {
                Logger.log("");
                Logger.log(LocaleUtil.getLocaleString("UnreferencedRecordCollector.ResultHeader",
                        "========== Unreferenced Records Check Results =========="));
                Logger.log(LocaleUtil.getFormatedLocaleString("UnreferencedRecordCollector.FoundTables",
                        "Found {0} table(s) with unreferenced records", tableToUnreferenced.size()));
                Logger.log(LocaleUtil.getFormatedLocaleString("UnreferencedRecordCollector.TotalRecords",
                        "Total {0} record(s) unreferenced", total));
                Logger.log(LocaleUtil.getLocaleString("UnreferencedRecordCollector.IdeallyZero",
                        "(Ideally 0, as entry/enum mechanisms can avoid magic numbers in code)"));
                Logger.log("");

                for (Map.Entry<String, List<UnreferencedRecord>> entry : tableToUnreferenced.entrySet()) {
                    String tableName = entry.getKey();
                    List<UnreferencedRecord> records = entry.getValue();
                    Logger.log(LocaleUtil.getFormatedLocaleString("UnreferencedRecordCollector.TableInfo",
                            "Table: {0} ({1} unreferenced)", tableName, records.size()));
                }
                Logger.log("==========");

                for (Map.Entry<String, List<UnreferencedRecord>> entry : tableToUnreferenced.entrySet()) {
                    String tableName = entry.getKey();
                    List<UnreferencedRecord> records = entry.getValue();
                    Logger.log(LocaleUtil.getFormatedLocaleString("UnreferencedRecordCollector.TableInfo",
                            "Table: {0} ({1} unreferenced)", tableName, records.size()));


                    int c = 0;
                    for (UnreferencedRecord record : records) {
                        if (c >= 50) {
                            Logger.log("...");
                            break;
                        }
                        record.print();
                        c++;
                    }
                }


                Logger.log(LocaleUtil.getLocaleString("UnreferencedRecordCollector.CheckComplete",
                        "========== Check Complete =========="));
            } else {
                Logger.log(LocaleUtil.getLocaleString("UnreferencedRecordCollector.AllReferenced",
                        "Unreferenced records check: All records are referenced, no unreferenced records found."));
            }
        }


    }


    public record UnreferencedRecord(String primaryKey, CfgValue.VStruct record) {

        public void print() {
            String titleFieldName = record.schema().meta().getStr("title", null);
            String title = null;
            if (titleFieldName != null) {
                title = ValueUtil.extractFieldValueStr(record, titleFieldName);
            }
            if (title != null) {
                Logger.log("  - %s, %s", primaryKey, title);
            } else {
                Logger.log("  - %s", primaryKey);
            }
        }

    }


    public static Unreferenced collectUnreferenced(CfgValue cfgValue) {
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());

        // 为每个 table 创建并发任务
        List<Callable<UnreferencedInTable>> tasks = new ArrayList<>();
        for (CfgValue.VTable vTable : cfgValue.sortedTables()) {
            tasks.add(() -> collectUnreferencedInTable(cfgValue, vTable, graph));
        }
        List<Future<UnreferencedInTable>> futures;
        try (ExecutorService executor = Executors.newWorkStealingPool()) {
            try {
                futures = executor.invokeAll(tasks);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // 收集结果
        Map<String, List<UnreferencedRecord>> allUnreferenced = new TreeMap<>();
        int totalUnreferencedCount = 0;
        for (Future<UnreferencedInTable> future : futures) {
            UnreferencedInTable result;
            try {
                result = future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            if (!result.unreferencedRecords().isEmpty()) {
                allUnreferenced.put(result.tableName(), result.unreferencedRecords());
                totalUnreferencedCount += result.unreferencedRecords().size();
            }
        }

        return new Unreferenced(totalUnreferencedCount, allUnreferenced);

    }

    public record UnreferencedInTable(String tableName,
                                      List<UnreferencedRecord> unreferencedRecords) {
    }

    public static UnreferencedInTable collectUnreferencedInTable(CfgValue cfgValue,
                                                                 CfgValue.VTable vTable,
                                                                 TableSchemaRefGraph graph) {
        List<UnreferencedRecord> unreferencedRecords = new ArrayList<>();
        ValueRefInCollector refInCollector = new ValueRefInCollector(graph, cfgValue);
        TableSchema tableSchema = vTable.schema();

        // 检查是否有entry，enum类型的entry
        String entryFieldName = null;
        if (tableSchema.entry() instanceof EntryType.EntryBase eEntry) {
            entryFieldName = eEntry.field();
        }

        // 遍历表中所有记录
        for (Map.Entry<CfgValue.Value, CfgValue.VStruct> e : vTable.primaryKeyMap().entrySet()) {
            CfgValue.Value pkValue = e.getKey();
            CfgValue.VStruct record = e.getValue();

            // EEntry特殊情况：如果record的entry字段有值，不算unreferenced
            if (entryFieldName != null) {
                CfgValue.Value entryFieldValue = ValueUtil.extractFieldValue(record, entryFieldName);
                if (entryFieldValue instanceof CfgValue.VString entry && !entry.value().isEmpty()) {
                    continue;
                }
            }

            // 检查是否被引用（使用优化的快速检查方法）
            if (!refInCollector.hasReference(vTable, pkValue)) {
                unreferencedRecords.add(new UnreferencedRecord(pkValue.packStr(), record));
            }
        }

        return new UnreferencedInTable(vTable.name(), unreferencedRecords);
    }
}
