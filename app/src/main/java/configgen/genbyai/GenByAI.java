package configgen.genbyai;

import com.alibaba.fastjson2.JSON;
import configgen.ctx.Context;
import configgen.data.CfgData;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.schema.*;
import configgen.value.*;
import configgen.write.VTableJsonStorage;
import configgen.write.VTableStorage;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.sashirestela.openai.domain.chat.ChatMessage.*;

public class GenByAI extends Generator {
    private final String cfgFn;
    private final String askFn;
    private final String table;
    private final int retryTimes;

    public GenByAI(Parameter parameter) {
        super(parameter);
        cfgFn = parameter.get("cfg", "ai.json");
        askFn = parameter.get("ask", "ask.txt");
        table = parameter.get("table", "skill.buff");
        retryTimes = Integer.parseInt(parameter.get("retry", "1"));
    }


    @Override
    public void generate(Context ctx) throws IOException {
        if (retryTimes <= 0) {
            throw new RuntimeException("retry must > 0");
        }

        AICfg aiCfg = AICfg.readFromFile(cfgFn);
        Path askFnPath = Path.of(askFn);
        if (!Files.exists(askFnPath)) {
            throw new RuntimeException(askFn + " not exist!");
        }
        List<String> asks;
        try {
            asks = Files.readAllLines(askFnPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        CfgValue cfgValue = ctx.makeValue();
        CfgValue.VTable vTable = cfgValue.getTable(table);
        Objects.requireNonNull(vTable, "table=%s not found!".formatted(table));

        PromptGen.Prompt prompt = PromptGen.genPrompt(ctx, cfgValue, vTable);
        System.out.println(prompt.prompt());
        System.out.println(prompt.init());

        // 调用llm
        SimpleOpenAI openAI = SimpleOpenAI.builder()
                .baseUrl(aiCfg.baseUrl())
                .apiKey(aiCfg.apiKey())
                .build();

        AskStat stat = new AskStat();
        for (String ask : asks) {
            if (ask.trim().isEmpty()) {
                continue;
            }

            System.out.println();
            System.out.printf("## %s%n", ask);
            List<ChatMessage> messages = List.of(
                    UserMessage.of(prompt.prompt()),
                    AssistantMessage.of(prompt.init()),
                    UserMessage.of(ask));
            askWithRetry(messages, retryTimes, stat,
                    ctx, vTable,
                    openAI, aiCfg.model());
        }
        System.out.println(stat);
    }

    private static class AskStat {
        int ask;
        int ok;
        int retry;

        int noJson;
        int err;
        int warn;

        @Override
        public String toString() {
            return "ask=%d, ok=%d, retry=%d, noJson=%d, err=%d, warn=%d".formatted(ask, ok, retry, noJson, err, warn);
        }
    }

    private void askWithRetry(List<ChatMessage> messages, int retryTimes, AskStat stat,
                              Context context, CfgValue.VTable vTable,
                              SimpleOpenAI openAI, String model) {
        stat.ask++;
        TableSchema tableSchema = vTable.schema();
        for (int i = 0; i < retryTimes; i++) {
            String jsonResult = ask(messages, openAI, model);
            if (i > 0) {
                stat.retry++;
            }

            if (jsonResult == null) {
                stat.noJson++;
                return;
            } else {
                CfgValueErrs parseErrs = CfgValueErrs.of();
                CfgValue.VStruct record = new ValueJsonParser(tableSchema, parseErrs).fromJson(jsonResult);
                parseErrs.checkErrors("check json", true, true);

                if (!parseErrs.warns().isEmpty()) {
                    stat.warn++;
                }
                if (parseErrs.errs().isEmpty()) {
                    stat.ok++;

                    CfgValue.Value pkValue = ValueUtil.extractPrimaryKeyValue(record, tableSchema);
                    String id = pkValue.packStr();
                    try {
                        if (tableSchema.isJson()) {
                            VTableJsonStorage.addOrUpdateRecord(record, tableSchema.name(), id,
                                    context.getSourceStructure().getRootDir());
                        } else {
                            CfgData.DTable dTable = context.cfgData().getDTable(table);
                            VTableStorage.addOrUpdateRecord(context, vTable, dTable, pkValue, record);
                        }

                    } catch (IOException e) {
                        System.out.printf("save %s err: %s%n", id, e.getMessage());
                    }
                    return;
                } else {
                    messages = new ArrayList<>(messages);
                    messages.add(UserMessage.of(PromptDefault.FIX_ERROR.formatted(parseErrs.toString())));

                    stat.err++;
                }
            }
        }
    }

    private String ask(List<ChatMessage> messages, SimpleOpenAI openAI, String model) {
        var chatRequest = ChatRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(0.0)
                .build();
        var futureChat = openAI.chatCompletions().create(chatRequest);
        var chatResponse = futureChat.join();
        String result = chatResponse.firstContent();
        System.out.println(result);
        System.out.println(chatResponse.getUsage().toString());
        return extractJson(result);
    }

    private static final Pattern pattern = Pattern.compile("```json\\s*([^`]+)\\s*```");

    public static String extractJson(String input) {
        Matcher matcher = pattern.matcher(input);
        String lastJson = null;
        while (matcher.find()) {
            lastJson = matcher.group(1).trim();
        }
        return lastJson;
    }

    public record AICfg(String baseUrl,
                        String apiKey,
                        String model) {
        public static AICfg readFromFile(String cfgFn) {
            Path path = Path.of(cfgFn);
            if (!Files.exists(path)) {
                throw new RuntimeException(cfgFn + " not exist!");
            }
            String jsonStr;
            try {
                jsonStr = Files.readString(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (jsonStr.isEmpty()) {
                throw new RuntimeException(cfgFn + " is empty!");
            }
            return JSON.parseObject(jsonStr, AICfg.class);
        }
    }

}



