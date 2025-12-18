package configgen.genbyai;

import configgen.ctx.Context;
import configgen.genbyai.PromptModel.Example;
import configgen.schema.*;
import configgen.schema.cfg.CfgWriter;
import configgen.util.MarkdownReader;
import configgen.value.*;
import configgen.value.CfgValue.VTable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TableRelatedInfoFinder {

    public record RelatedInfo(String relatedSchema,
                              List<TableRecordList> relatedTableRecordListInCsv,
                              List<TableCount> otherTableCounts,
                              String rule,
                              Example example) {

        public String asTextContent() {
            StringBuilder sb = new StringBuilder(2048);
            sb.append("<related_schema>\n");
            sb.append("```\n");
            sb.append(relatedSchema);
            sb.append("```\n</related_schema>\n\n");

            for (TableRecordList info : relatedTableRecordListInCsv) {
                sb.append("```csv table=%s recordCount=%d\n".formatted(info.table, info.recordCount));
                sb.append(info.contentInCsvFormat);
                sb.append("```\n\n");
            }

            if (!otherTableCounts.isEmpty()) {
                sb.append("```csv table,recordCount\n");
                for (TableCount info : otherTableCounts) {
                    sb.append("%s,%d\n".formatted(info.table(), info.recordCount()));
                }
                sb.append("```\n\n");
            }

            if (rule != null && !rule.isBlank()) {
                sb.append("<rule>\n");
                sb.append(rule);
                sb.append("</rule>\n\n");
            }

            if (example != null) {
                sb.append("<example>\n");
                sb.append(example.toPrompt());
                sb.append("</example>\n\n");
            }

            return sb.toString();
        }
    }

    public record TableRecordList(String table,
                                  int recordCount,
                                  String contentInCsvFormat) {
    }

    public record TableCount(String table,
                             int recordCount) {
    }

    public record ModuleRule(String description,
                             String rule) {
    }

    public record TableRule(String rule,
                            List<String> extraRefTables,
                            String exampleId,
                            String exampleDescription) {
    }


    public static RelatedInfo findRelatedInfo(Context context,
                                              CfgValue cfgValue,
                                              VTable vTable) {
        TableSchema tableSchema = vTable.schema();
        String relatedCfg = findRelatedCfgStr(tableSchema);
        TableRule rule = findTableRule(context, tableSchema);


        RelatedInfo relatedInfo = new RelatedInfo(relatedCfg, new ArrayList<>(), new ArrayList<>(),
                rule != null ? rule.rule : null,
                getExample(rule, vTable));

        var refOutTables = TableSchemaRefGraph.findAllRefOuts(tableSchema);
        List<String> extraRefTables = rule != null ? rule.extraRefTables() : List.of();
        for (TableSchema schema : refOutTables.values()) {
            VTable vt = cfgValue.getTable(schema.name());
            if (schema.entry() instanceof EntryType.EEnum || extraRefTables.contains(schema.name())) {
                relatedInfo.relatedTableRecordListInCsv.add(getTableRecordListInCsv(vt, null, 0, 20));
            } else {
                relatedInfo.otherTableCounts.add(new TableCount(schema.name(), vt.valueList().size()));
            }
        }
        return relatedInfo;
    }


    public static ModuleRule findModuleRuleForTable(Context context, TableSchema tableSchema) {
        String namespace = tableSchema.namespace();
        Path cfgFilePath = context.sourceStructure().getCfgFilePathByPkgName(namespace);
        if (cfgFilePath == null) {
            return null;
        }

        // $mod.md
        Path modDir = cfgFilePath.getParent();
        String moduleRule = null;
        Path modFile = modDir.resolve("$mod.md");
        if (!Files.exists(modFile)) {
            return null;
        }

        MarkdownReader.MarkdownDocument doc = MarkdownReader.read(modFile);
        String description = doc.frontmatter().get("description");
        moduleRule = doc.content();
        return new ModuleRule(description, moduleRule);
    }

    public static TableRule findTableRule(Context context, TableSchema tableSchema) {
        String namespace = tableSchema.namespace();
        Path cfgFilePath = context.sourceStructure().getCfgFilePathByPkgName(namespace);
        if (cfgFilePath == null) {
            return null;
        }
        Path modDir = cfgFilePath.getParent();

        // 找对应table的md
        String rule = null;
        List<String> extraRefTables = new ArrayList<>();
        String exampleId = null;
        String exampleDescription = null;
        Path tabFile = modDir.resolve(tableSchema.lastName() + ".md");
        if (Files.exists(tabFile)) {
            MarkdownReader.MarkdownDocument doc = MarkdownReader.read(tabFile);
            String refTables = doc.frontmatter().get("refTables");
            if (refTables != null && !refTables.isBlank()) {
                String trim = refTables.trim();
                extraRefTables.addAll(Arrays.asList(trim.split("[;,]")));
            }
            exampleId = doc.frontmatter().get("exampleId");
            exampleDescription = doc.frontmatter().get("exampleDescription");


            rule = doc.content();
        }

        return new TableRule(rule, extraRefTables, exampleId, exampleDescription);
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

    public static TableRecordList getTableRecordListInCsv(VTable vTable, String[] extraFields, int offset, int limit) {
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

        if (extraFields != null) {
            for (String extraField : extraFields) {
                if (vTable.schema().findField(extraField) != null) {
                    fieldNames.add(extraField);
                }
            }
        }

        ValueToCsv.writeAsCsv(sb, vTable, fieldNames, offset, limit);

        return new TableRecordList(schema.name(), vTable.valueList().size(), sb.toString());
    }


    public static Example getExample(TableRule rule, VTable vTable) {
        if (rule == null || vTable == null) {
            return null;
        }
        if (rule.exampleId() == null || rule.exampleId().isBlank()
                || rule.exampleDescription() == null || rule.exampleDescription().isBlank()) {
            return null;
        }
        CfgValueErrs errs = CfgValueErrs.of();
        CfgValue.Value pkValue = ValuePack.unpackTablePrimaryKey(rule.exampleId(), vTable.schema(), errs);
        if (errs.errs().isEmpty()) {
            CfgValue.VStruct vRecord = vTable.primaryKeyMap().get(pkValue);
            if (vRecord != null) {
                String jsonString = ValueToJson.toJsonStr(vRecord);
                return new Example(rule.exampleId(), rule.exampleDescription(), jsonString);
            }
        }

        return null;
    }

}
