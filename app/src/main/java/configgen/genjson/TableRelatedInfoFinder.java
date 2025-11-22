package configgen.genjson;

import configgen.schema.*;
import configgen.schema.cfg.CfgWriter;
import configgen.value.CfgValue;
import configgen.value.ValueToCsv;

import java.util.*;

public class TableRelatedInfoFinder {

    public record TableBrief(String table,
                             String contentInCsvFormat) {
    }

    public record TableCount(String table,
                             int recordCount) {
    }

    public record RelatedInfo(String relatedCfg,
                              List<TableBrief> relatedTableCsv,
                              List<TableCount> otherTableCounts) {

        public String prompt() {
            StringBuilder sb = new StringBuilder(2048);
            sb.append("<related_schema>\n");
            sb.append("```\n");
            sb.append(relatedCfg);
            sb.append("```\n</related_schema>\n");

            for (TableBrief info : relatedTableCsv) {
                sb.append("```csv brief table=%s\n".formatted(info.table));
                sb.append(info.contentInCsvFormat);
                sb.append("```\n\n");
            }

            if (!otherTableCounts.isEmpty()) {
                sb.append("```csv table record_count\n");
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
                                              List<String> extraRefEnumTables) {
        String relatedCfg = findRelatedCfgStr(tableSchema);
        RelatedInfo relatedInfo = new RelatedInfo(relatedCfg, new ArrayList<>(), new ArrayList<>());
        fillRelatedTableInfo(relatedInfo, cfgValue, tableSchema, extraRefEnumTables);
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

    private static void fillRelatedTableInfo(RelatedInfo relatedInfo,
                                             CfgValue cfgValue,
                                             TableSchema tableSchema,
                                             List<String> extraRefTables) {

        var refOutTables = TableSchemaRefGraph.findAllRefOuts(tableSchema);
        for (TableSchema schema : refOutTables.values()) {
            CfgValue.VTable vTable = cfgValue.getTable(schema.name());
            if (schema.entry() instanceof EntryType.EEnum en || extraRefTables.contains(schema.name())) {
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
                ValueToCsv.writeAsCsv(sb, vTable, fieldNames, "");

                relatedInfo.relatedTableCsv.add(new TableBrief(schema.name(), sb.toString()));
            } else {
                relatedInfo.otherTableCounts.add(new TableCount(schema.name(), vTable.valueList().size()));
            }
        }
    }

}
