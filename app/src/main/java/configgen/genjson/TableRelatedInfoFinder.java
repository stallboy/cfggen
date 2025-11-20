package configgen.genjson;

import configgen.schema.*;
import configgen.schema.cfg.CfgWriter;
import configgen.value.CfgValue;
import configgen.value.ValueToCsv;

import java.util.*;

public class TableRelatedInfoFinder {

    record EnumTableInfo(String table,
                         String contentInCsvFormat) {
    }

    record TableBrief(String table,
                      int recordCount) {
    }

    record RelatedInfo(String relatedCfg,
                       List<EnumTableInfo> enumCsv,
                       List<TableBrief> refOutTableBriefs) {
    }

    public static String findRelatedCfgStr(TableSchema tableSchema) {
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

    public static String findRelatedEnumCsv(CfgValue cfgValue, TableSchema tableSchema, List<String> extraRefTables) {
        StringBuilder sb = new StringBuilder(2048);
        var refOutTables = TableSchemaRefGraph.findAllRefOuts(tableSchema);
        for (TableSchema refOutTable : refOutTables.values()) {
            boolean use = false;
            EntryType.EEnum eEnum = null;
            if (refOutTable.entry() instanceof EntryType.EEnum en) {
                eEnum = en;
                use = true;
            } else if (extraRefTables.contains(refOutTable.name())) {
                use = true;
            }
            if (use) {
                sb.append("- %s%n".formatted(refOutTable.name()));
                sb.append("```%n".formatted());
                addOneRelatedCsv(sb, cfgValue, refOutTable, eEnum);
                sb.append("```%n%n".formatted());
            }
        }

        return sb.toString();
    }

    private static void addOneRelatedCsv(StringBuilder sb, CfgValue cfgValue, TableSchema tableSchema, EntryType.EEnum en) {
        Set<String> fieldNames = new LinkedHashSet<>(tableSchema.primaryKey().fields());
        if (en != null) {
            fieldNames.add(en.field());
        }
        String title = tableSchema.meta().getStr("title", null);
        if (title != null) {
            fieldNames.add(title);
        }

        CfgValue.VTable vTable = cfgValue.getTable(tableSchema.name());
        ValueToCsv.writeAsCsv(sb, vTable, fieldNames, "");
    }

}
