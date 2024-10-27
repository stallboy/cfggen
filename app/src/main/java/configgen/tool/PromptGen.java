package configgen.tool;

import configgen.schema.*;
import configgen.schema.cfg.CfgWriter;
import configgen.value.*;
import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;

import java.nio.file.Path;
import java.util.*;

import static configgen.tool.AICfg.*;

public class PromptGen {


    public static String genPrompt(CfgValue cfgValue,
                                   String table,
                                   String promptFile,
                                   TableCfg nullableTableCfg,
                                   boolean isUseRawStructInfo) {
        CfgValue.VTable vTable = cfgValue.getTable(table);
        Objects.requireNonNull(vTable, "table=%s not found!".formatted(table));
        TableSchema tableSchema = vTable.schema();

        // 创建model
        String structInfo;
        String extra;
        List<String> extraRefTables = nullableTableCfg != null ? nullableTableCfg.extraRefTables() : List.of();
        List<OneExample> examples = nullableTableCfg != null ? nullableTableCfg.examples() : List.of();
        if (isUseRawStructInfo) {
            structInfo = getTableSchemaRelatedCfgStr(tableSchema);
            extra = getTableRelatedEnumCsv(cfgValue, tableSchema, extraRefTables);
        } else {
            structInfo = new SchemaToTs(cfgValue, tableSchema, extraRefTables, true).generate();
            extra = "";
        }
        PromptModel model = new PromptModel(table, structInfo, extra, getExamples(examples, vTable));

        // 生成prompt
        CodeResolver codeResolver = new DirectoryCodeResolver(Path.of("."));
        TemplateEngine templateEngine = TemplateEngine.create(codeResolver, ContentType.Plain);
        TemplateOutput prompt = new StringOutput();
        templateEngine.render(promptFile, model, prompt);

        return prompt.toString();
    }


    private static String getTableSchemaRelatedCfgStr(TableSchema tableSchema) {
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

    private static String getTableRelatedEnumCsv(CfgValue cfgValue, TableSchema tableSchema, List<String> extraRefTables) {
        StringBuilder sb = new StringBuilder(2048);
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());
        Collection<TableSchema> refOutTables = graph.getRefOutTables(tableSchema);
        if (refOutTables != null) {
            for (TableSchema refOutTable : refOutTables) {
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

    private static List<PromptModel.Example> getExamples(List<OneExample> rawExamples, CfgValue.VTable vTable) {
        List<PromptModel.Example> examples = new ArrayList<>(rawExamples.size());
        for (OneExample ex : rawExamples) {
            ValueErrs errs = ValueErrs.of();
            CfgValue.Value pkValue = ValuePack.unpackTablePrimaryKey(ex.id(), vTable.schema(), errs);

            if (errs.errs().isEmpty()) {
                CfgValue.VStruct vRecord = vTable.primaryKeyMap().get(pkValue);
                if (vRecord != null) {
                    String jsonString = ValueToJson.toJsonStr(vRecord);
                    examples.add(new PromptModel.Example(ex.id(), ex.description(), jsonString));
                }
            }
        }
        return examples;
    }
}
