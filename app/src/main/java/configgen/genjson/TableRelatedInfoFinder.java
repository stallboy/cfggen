package configgen.genjson;

import configgen.schema.*;
import configgen.schema.cfg.CfgWriter;
import configgen.value.CfgValue;
import configgen.value.ValueToCsv;

import java.util.*;

public class TableRelatedInfoFinder {

    public record TableRecordList(String table,
                                  String contentInCsvFormat) {

        public void prompt(StringBuilder sb) {
            sb.append("```csv table=%s record list\n".formatted(table));
            sb.append(contentInCsvFormat);
            sb.append("```\n\n");
        }

        public String prompt() {
            StringBuilder sb = new StringBuilder(512);
            prompt(sb);
            return sb.toString();
        }
    }

    public record TableCount(String table,
                             int recordCount) {
    }

    public record RelatedInfo(String relatedCfg,
                              List<TableRecordList> relatedTableRecordListInCsv,
                              List<TableCount> otherTableCounts) {

        public String prompt() {
            StringBuilder sb = new StringBuilder(2048);
            sb.append("<related_schema>\n");
            sb.append("```\n");
            sb.append(relatedCfg);
            sb.append("```\n</related_schema>\n\n");

            for (TableRecordList info : relatedTableRecordListInCsv) {
                info.prompt(sb);
            }

            if (!otherTableCounts.isEmpty()) {
                sb.append("```csv table, record count\n");
                for (TableCount info : otherTableCounts) {
                    sb.append("%s,%d records\n".formatted(info.table(), info.recordCount()));
                }
                sb.append("```\n");
            }

            return sb.toString();
        }
    }

    public static RelatedInfo findRelatedInfo(CfgValue cfgValue,
                                              TableSchema tableSchema,
                                              List<String> extraRefTables) {
        String relatedCfg = findRelatedCfgStr(tableSchema);
        RelatedInfo relatedInfo = new RelatedInfo(relatedCfg, new ArrayList<>(), new ArrayList<>());
        var refOutTables = TableSchemaRefGraph.findAllRefOuts(tableSchema);
        for (TableSchema schema : refOutTables.values()) {
            CfgValue.VTable vTable = cfgValue.getTable(schema.name());
            if (schema.entry() instanceof EntryType.EEnum || extraRefTables.contains(schema.name())) {
                relatedInfo.relatedTableRecordListInCsv.add(getTableRecordListInCsv(vTable, null));
            } else {
                relatedInfo.otherTableCounts.add(new TableCount(schema.name(), vTable.valueList().size()));
            }
        }
        return relatedInfo;
    }

    private static String findRelatedCfgStr(TableSchema tableSchema) {
        StringBuilder sb = new StringBuilder(2048);
        Map<String, Nameable> allIncludedStructs = IncludedStructs.findAllIncludedStructs(tableSchema);
        for (Nameable s : allIncludedStructs.values()) {
            if (s instanceof StructSchema ss && ss.nullableInterface() != null) {
                continue;
            }
            CfgWriter cfgWriter = new CfgWriter(sb, false, false);
            cfgWriter.writeNamable(s, "");
        }
        return sb.toString();
    }

    public static TableRecordList getTableRecordListInCsv(CfgValue.VTable vTable, List<String> extraFields) {
        TableSchema schema = vTable.schema();

        StringBuilder sb = new StringBuilder(2048);
        Set<String> fieldNames = new LinkedHashSet<>(2);
        fieldNames.addAll(schema.primaryKey().fields());
        if (schema.entry() instanceof EntryType.EEnum eEnum) {
            fieldNames.add(eEnum.field());
        }
        String title = schema.meta().getStr("title", null);
        if (title != null) {
            fieldNames.add(title);
        }

        if (extraFields != null){
            for (String extraField : extraFields) {
                if (vTable.schema().findField(extraField) != null){
                    fieldNames.add(extraField);
                }
            }
        }

        ValueToCsv.writeAsCsv(sb, vTable, fieldNames);

        return new TableRecordList(schema.name(), sb.toString());
    }

}
