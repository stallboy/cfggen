package configgen.value;

import configgen.schema.TableSchema;
import configgen.util.LocaleUtil;
import configgen.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.schema.EntryType.EEntry;
import static configgen.schema.EntryType.EEnum;

public class EntryRecordCollector {

    public enum EntryTypeTag {
        ROOT, ENUM, ENTRY
    }

    public record Entry(int total,
                        List<EntryInTable> tables) {

        public void print() {
            if (total > 0) {
                Logger.log("");
                Logger.log(LocaleUtil.getLocaleString("EntryRecordCollector.ResultHeader",
                        "========== Entry Records Check Results =========="));
                Logger.log(LocaleUtil.getFormatedLocaleString("EntryRecordCollector.FoundTables",
                        "Found {0} table(s) with entry records", tables.size()));
                Logger.log(LocaleUtil.getFormatedLocaleString("EntryRecordCollector.TotalRecords",
                        "Total {0} record(s) as entry points", total));
                Logger.log("");

                for (EntryInTable entryInTable : tables) {
                    String tableName = entryInTable.tableName();
                    List<EntryRecord> records = entryInTable.entryRecords();
                    EntryTypeTag typeTag = entryInTable.typeTag();
                    String typeStr = switch (typeTag) {
                        case ROOT -> "[ROOT]";
                        case ENUM -> "[ENUM]";
                        case ENTRY -> "[ENTRY]";
                    };
                    Logger.log(LocaleUtil.getFormatedLocaleString("EntryRecordCollector.TableInfo",
                            "Table: {0} {1} ({2} entry records)", tableName, typeStr, records.size()));

                    int c = 0;
                    for (EntryRecord record : records) {
                        if (c >= 10) {
                            Logger.log("...");
                            break;
                        }
                        record.print();
                        c++;
                    }
                }

                Logger.log(LocaleUtil.getLocaleString("EntryRecordCollector.CheckComplete",
                        "========== Check Complete =========="));
            } else {
                Logger.log(LocaleUtil.getLocaleString("EntryRecordCollector.NoEntry",
                        "Entry records check: No entry records found."));
            }
        }
    }

    public record EntryInTable(String tableName,
                               List<EntryRecord> entryRecords,
                               EntryTypeTag typeTag) {
    }

    public record EntryRecord(String primaryKey,
                              CfgValue.VStruct record) {

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


    public static Entry collectEntry(CfgValue cfgValue) {
        List<EntryInTable> tables = new ArrayList<>();
        int totalEntryCount = 0;

        // 直接遍历所有表
        for (CfgValue.VTable vTable : cfgValue.sortedTables()) {
            EntryInTable result = collectEntryInTable(vTable);
            if (!result.entryRecords().isEmpty()) {
                tables.add(result);
                totalEntryCount += result.entryRecords().size();
            }
        }

        return new Entry(totalEntryCount, tables);
    }

    public static EntryInTable collectEntryInTable(CfgValue.VTable vTable) {
        List<EntryRecord> entryRecords = new ArrayList<>();
        TableSchema tableSchema = vTable.schema();

        EntryTypeTag typeTag;
        String entryFieldName = null;
        boolean collectAll = false;

        // 判断表类型
        if (tableSchema.meta().isRoot()) {
            typeTag = EntryTypeTag.ROOT;
            collectAll = true;
        } else {
            switch (tableSchema.entry()) {
                case EEnum ignored -> {
                    // enum 类型：收集所有记录
                    typeTag = EntryTypeTag.ENUM;
                    collectAll = true;
                }
                case EEntry eEntry -> {
                    // EEntry 类型：只收集 entry 字段有值的记录
                    typeTag = EntryTypeTag.ENTRY;
                    entryFieldName = eEntry.field();
                }
                default -> {
                    // ENo 类型：不收集任何记录，直接返回空结果
                    return new EntryInTable(vTable.name(), entryRecords, EntryTypeTag.ENTRY);
                }
            }
        }

        // 遍历表中所有记录
        for (Map.Entry<CfgValue.Value, CfgValue.VStruct> e : vTable.primaryKeyMap().entrySet()) {
            CfgValue.VStruct record = e.getValue();

            if (collectAll) {
                // ROOT 或 enum 类型：收集所有记录
                entryRecords.add(new EntryRecord(e.getKey().packStr(), record));
            } else if (entryFieldName != null) {
                // EEntry 类型：检查 entry 字段是否有值
                CfgValue.Value entryFieldValue = ValueUtil.extractFieldValue(record, entryFieldName);
                if (entryFieldValue instanceof CfgValue.VString entry && !entry.value().isEmpty()) {
                    entryRecords.add(new EntryRecord(e.getKey().packStr(), record));
                }
            }
        }

        return new EntryInTable(vTable.name(), entryRecords, typeTag);
    }
}
