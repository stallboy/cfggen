package configgen.tool;

import com.alibaba.fastjson2.JSON;
import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.value.CfgValue;
import configgen.value.ValueErrs;
import configgen.value.ValuePack;
import configgen.value.ValueToJson;
import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.ChatRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.github.sashirestela.openai.domain.chat.ChatMessage.*;

public class GenJsonByAI extends Generator {
    public record ExampleDesc(
            String table,
            String id,
            String description) {
    }

    public record JsonByAICfg(
            String baseUrl,
            String apiKey,
            String model,
            List<ExampleDesc> examples) {
    }

    public record Example(
            String id,
            String description,
            String json) {
    }

    public record PromptModel(
            String structInfo,
            List<Example> examples) {
    }

    private static final String ANSWER = "请提供ID和描述，我将根据这些信息生成符合结构的JSON配置。";

    private final JsonByAICfg cfg;
    private final List<String> asks;
    private final String table;

    public GenJsonByAI(Parameter parameter) {
        super(parameter);
        String cfgFn = parameter.get("cfg", "ai.json");
        String askFn = parameter.get("ask", "ask.txt");
        table = parameter.get("table", "skill.buff");

        if (tag != null) {
            throw new RuntimeException("gen jsonByAI with tag=%s not supported".formatted(tag));
        }
        if (!Files.exists(Path.of(cfgFn))) {
            throw new RuntimeException(cfgFn + " not exist!");
        }
        if (!Files.exists(Path.of(askFn))) {
            throw new RuntimeException(askFn + " not exist!");
        }
        String promptFn = table + ".jte";
        if (!Files.exists(Path.of(promptFn))) {
            throw new RuntimeException(promptFn + " not exist!");
        }

        String jsonStr;
        try {
            jsonStr = Files.readString(Path.of(cfgFn));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cfg = JSON.parseObject(jsonStr, JsonByAICfg.class);

        try {
            asks = Files.readAllLines(Path.of(askFn));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue();
        CfgValue.VTable vTable = cfgValue.getTable(table);
        Objects.requireNonNull(vTable, "table=%s not found!".formatted(table));

        // 创建model
        String structInfo = new SchemaToTs(cfgValue, vTable.schema()).generate();
        List<Example> examples = new ArrayList<>(cfg.examples.size());
        for (ExampleDesc ex : cfg.examples) {

            ValueErrs errs = ValueErrs.of();
            CfgValue.Value pkValue = ValuePack.unpackTablePrimaryKey(ex.id, vTable.schema(), errs);

            if (errs.errs().isEmpty()) {
                CfgValue.VStruct vRecord = vTable.primaryKeyMap().get(pkValue);
                if (vRecord != null) {
                    String jsonString = ValueToJson.toJsonStr(vRecord);
                    examples.add(new Example(ex.id, ex.description, jsonString));
                }
            }
        }
        PromptModel model = new PromptModel(structInfo, examples);


        // 生成prompt
        CodeResolver codeResolver = new DirectoryCodeResolver(Path.of("."));
        TemplateEngine templateEngine = TemplateEngine.create(codeResolver, ContentType.Plain);
        String promptFn = table + ".jte";
        TemplateOutput prompt = new StringOutput();
        templateEngine.render(promptFn, model, prompt);
        System.out.println(prompt);

        // 调用llm
        var openAI = SimpleOpenAI.builder()
                .baseUrl(cfg.baseUrl)
                .apiKey(cfg.apiKey)
                .build();

        for (String ask : asks) {
            if (ask.trim().isEmpty()) {
                continue;
            }

            System.out.println(ask);
            var chatRequest = ChatRequest.builder()
                    .model(cfg.model)
                    .message(UserMessage.of(prompt.toString()))
                    .message(AssistantMessage.of(ANSWER))
                    .message(UserMessage.of(ask))
                    .temperature(0.0)
                    .build();
            var futureChat = openAI.chatCompletions().create(chatRequest);
            var chatResponse = futureChat.join();
            System.out.println(chatResponse.firstContent());
        }

    }
}


