package configgen.genbyai;

import configgen.ctx.Context;
import configgen.genbyai.TableRelatedInfoFinder.Rule;
import configgen.schema.*;
import configgen.util.JteEngine;
import configgen.util.MarkdownReader;
import configgen.util.MarkdownReader.MarkdownDocument;
import configgen.value.*;
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

        Rule rule = TableRelatedInfoFinder.findRule(context, tableSchema);


        String structInfo = new SchemaToTs(cfgValue, tableSchema,
                rule != null ? rule.extraRefTables() : List.of(),
                true).generate();

        PromptModel.Example example = TableRelatedInfoFinder.getExample(rule, vTable);
        PromptModel model = new PromptModel(table, structInfo,
                rule != null ? rule.combineRule() : "",
                example != null ? List.of(example) : List.of());

        // 生成prompt
        Path rootDir = context.getSourceStructure().getRootDir();
        String prompt = JteEngine.renderTryFileFirst(rootDir.resolve("config.jte").toString(),
                "config.jte", model);

        String init = PromptDefault.DEFAULT_INIT;
        Path initFile = rootDir.resolve("init.md");
        if (Files.exists(initFile)) {
            MarkdownDocument doc = MarkdownReader.read(initFile);
            String c = doc.content();
            if (!c.isBlank()) {
                init = c.trim();
            }
        }

        return new Prompt(prompt, init);
    }
}
