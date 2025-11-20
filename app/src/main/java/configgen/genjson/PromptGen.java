package configgen.genjson;

import configgen.schema.*;
import configgen.value.*;
import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;

import java.nio.file.Path;
import java.util.*;

import static configgen.genjson.AICfg.*;

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
            structInfo = TableRelatedInfoFinder.findRelatedCfgStr(tableSchema);
            extra = TableRelatedInfoFinder.findRelatedEnumCsv(cfgValue, tableSchema, extraRefTables);
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


    private static List<PromptModel.Example> getExamples(List<OneExample> rawExamples, CfgValue.VTable vTable) {
        List<PromptModel.Example> examples = new ArrayList<>(rawExamples.size());
        for (OneExample ex : rawExamples) {
            CfgValueErrs errs = CfgValueErrs.of();
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
