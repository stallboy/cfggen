package configgen.genjson;

import configgen.ctx.Context;
import configgen.schema.*;
import configgen.util.JteEngine;
import configgen.util.MarkdownReader;
import configgen.util.MarkdownReader.MarkdownDocument;
import configgen.value.*;
import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PromptGen {

    public record Prompt(@NotNull String prompt,
                         @NotNull String init) {

    }

    public static @NotNull Prompt genPrompt(@NotNull Context context,
                                            @NotNull CfgValue cfgValue,
                                            @NotNull CfgValue.VTable vTable) {
        String table = vTable.name();
        TableSchema tableSchema = vTable.schema();
        String namespace = tableSchema.namespace();
        Path cfgFilePath = context.getSourceStructure().getCfgFilePathByPkgName(namespace);
        if (cfgFilePath == null) {
            throw new IllegalArgumentException("SHOULD NOT HAPPEN, cfg file path not found");
        }

        // 先找<mod>.md
        Path modDir = cfgFilePath.getParent();
        StringBuilder rule = new StringBuilder();
        Path modFile = modDir.resolve("$mod.md");
        if (Files.exists(modFile)) {
            MarkdownDocument doc = MarkdownReader.read(modFile);
            rule.append(doc.content());
        }

        // 再找对应table的md
        List<String> extraRefTables = new ArrayList<>();
        List<OneExample> examples = new ArrayList<>();
        Path tabFile = modDir.resolve(tableSchema.lastName() + ".md");
        if (Files.exists(tabFile)) {
            MarkdownDocument doc = MarkdownReader.read(tabFile);
            String refTables = doc.frontmatter().get("refTables");
            if (refTables != null && !refTables.isBlank()) {
                String trim = refTables.trim();
                extraRefTables.addAll(Arrays.asList(trim.split(";")));
            }
            String exampleId = doc.frontmatter().get("exampleId");
            String exampleDescription = doc.frontmatter().get("exampleDescription");

            if (exampleId != null && exampleDescription != null &&
                    !exampleId.isBlank() && !exampleDescription.isBlank()) {
                examples.add(new OneExample(exampleId.trim(), exampleDescription.trim()));
            }
            rule.append(doc.content());
        }

        String structInfo = new SchemaToTs(cfgValue, tableSchema, extraRefTables, true).generate();
        PromptModel model = new PromptModel(table, structInfo, rule.toString().trim(), getExamples(examples, vTable));

        // 生成prompt
        TemplateOutput prompt = new StringOutput();
        Path rootDir = context.getSourceStructure().getRootDir();
        if (Files.exists(rootDir.resolve("config.jte"))) {
            CodeResolver codeResolver = new DirectoryCodeResolver(rootDir);
            TemplateEngine templateEngine = TemplateEngine.create(codeResolver, ContentType.Plain);
            templateEngine.render("config.jte", model, prompt);
        } else { //内置的
            JteEngine.render("config.jte", model, prompt);
        }

        String init = PromptDefault.DEFAULT_INIT;
        Path initFile = rootDir.resolve("init.md");
        if (Files.exists(initFile)) {
            MarkdownDocument doc = MarkdownReader.read(initFile);
            String c = doc.content();
            if (!c.isBlank()) {
                init = c.trim();
            }
        }

        return new Prompt(prompt.toString(), init);
    }

    private record OneExample(String id,
                              String description) {
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
