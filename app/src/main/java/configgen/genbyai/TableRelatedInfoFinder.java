package configgen.genbyai;

import configgen.ctx.Context;
import configgen.schema.*;
import configgen.schema.cfg.CfgWriter;
import configgen.util.MarkdownReader;
import configgen.value.*;
import configgen.value.CfgValue.VTable;

import java.nio.file.Files;
import java.nio.file.Path;
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
                              List<TableCount> otherTableCounts,
                              String rule,
                              String example) {

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

            if (rule != null && !rule.isBlank()) {
                sb.append("<rule>\n");
                sb.append(rule);
                sb.append("</rule>\n\n");
            }

            if (example != null && !example.isBlank()) {
                sb.append("<example>\n");
                sb.append(example);
                sb.append("</example>\n\n");
            }

            return sb.toString();
        }
    }

    public record Rule(String rule,
                       String moduleRule,
                       List<String> extraRefTables,
                       String exampleId,
                       String exampleDescription) {

        public String getRule() {
            if (rule == null && moduleRule == null) {
                return "";
            }
            if (rule == null) {
                return moduleRule.trim();
            }

            if (moduleRule == null) {
                return rule.trim();
            }

            return moduleRule.trim() + "\n\n" + rule.trim();
        }
    }


    public static RelatedInfo findRelatedInfo(Context context,
                                              CfgValue cfgValue,
                                              VTable vTable) {
        TableSchema tableSchema = vTable.schema();
        String relatedCfg = findRelatedCfgStr(tableSchema);
        Rule rule = findRule(context, tableSchema);
        String ruleStr = null;
        String exampleStr = null;
        if (rule != null) {
            ruleStr = rule.getRule();
            var example = getExample(rule, vTable);
            if (example != null) {
                exampleStr = example.toPrompt();
            }
        }
        List<String> extraRefTables = rule != null ? rule.extraRefTables() : List.of();

        RelatedInfo relatedInfo = new RelatedInfo(relatedCfg, new ArrayList<>(), new ArrayList<>(), ruleStr, exampleStr);
        var refOutTables = TableSchemaRefGraph.findAllRefOuts(tableSchema);
        for (TableSchema schema : refOutTables.values()) {
            VTable vt = cfgValue.getTable(schema.name());
            if (schema.entry() instanceof EntryType.EEnum || extraRefTables.contains(schema.name())) {
                relatedInfo.relatedTableRecordListInCsv.add(getTableRecordListInCsv(vt, null));
            } else {
                relatedInfo.otherTableCounts.add(new TableCount(schema.name(), vt.valueList().size()));
            }
        }
        return relatedInfo;
    }


    public static Rule findRule(Context context, TableSchema tableSchema) {
        String namespace = tableSchema.namespace();
        Path cfgFilePath = context.getSourceStructure().getCfgFilePathByPkgName(namespace);
        if (cfgFilePath == null) {
            return null;
        }
        // 先找<mod>.md
        Path modDir = cfgFilePath.getParent();
        String moduleRule = null;
        Path modFile = modDir.resolve("$mod.md");
        if (Files.exists(modFile)) {
            MarkdownReader.MarkdownDocument doc = MarkdownReader.read(modFile);
            moduleRule = doc.content();
        }

        // 再找对应table的md
        String rule = null;
        List<String> extraRefTables = new ArrayList<>();
        String exampleId = null;
        String exampleDescription = null;
        Path tabFile = modDir.resolve(tableSchema.lastName() + ".md");
        extraRefTables.add(tableSchema.name()); // 把自己加上，方便自动生成下一个id（rule里可以给id生成规则）
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
        if (rule == null && moduleRule == null) {
            return null;
        }
        return new Rule(rule, moduleRule, extraRefTables, exampleId, exampleDescription);
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

    public static TableRecordList getTableRecordListInCsv(VTable vTable, List<String> extraFields) {
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

        ValueToCsv.writeAsCsv(sb, vTable, fieldNames);

        return new TableRecordList(schema.name(), sb.toString());
    }


    public static PromptModel.Example getExample(Rule rule, VTable vTable) {
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
                return new PromptModel.Example(rule.exampleId(), rule.exampleDescription(), jsonString);
            }
        }

        return null;
    }

}
