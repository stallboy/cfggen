package configgen.genjson;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.schema.*;
import configgen.value.*;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static configgen.genjson.AICfg.*;
import static io.github.sashirestela.openai.domain.chat.ChatMessage.*;

public class GenJsonByAI extends Generator {
    private final String cfgFn;
    private final String askFn;
    private final String table;
    private final String promptFn;
    private final boolean isUseRawStructInfo;
    private final int retryTimes;

    public GenJsonByAI(Parameter parameter) {
        super(parameter);
        cfgFn = parameter.get("cfg", "ai.json");
        askFn = parameter.get("ask", "ask.txt");
        table = parameter.get("table", "skill.buff");
        promptFn = parameter.get("promptfn", null);
        isUseRawStructInfo = parameter.has("raw");
        retryTimes = Integer.parseInt(parameter.get("retry", "1"));
    }

    @Override
    public void generate(Context ctx) throws IOException {
        if (retryTimes <= 0) {
            throw new RuntimeException("retry must > 0");
        }

        AICfg aiCfg = readFromFile(cfgFn);
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

        String promptFile;
        if (promptFn == null) {
            promptFile = aiCfg.assureFindPromptFile(table, Path.of(cfgFn).getParent());
        } else {
            promptFile = promptFn;
            if (!Files.exists(Path.of(promptFile))) {
                throw new RuntimeException(promptFile + " not exist!");
            }
        }

        CfgValue cfgValue = ctx.makeValue();
        CfgValue.VTable vTable = cfgValue.getTable(table);
        Objects.requireNonNull(vTable, "table=%s not found!".formatted(table));
        TableSchema tableSchema = vTable.schema();
        TableCfg aiTableCfg = aiCfg.findTable(table);

        String prompt = PromptGen.genPrompt(cfgValue, table, promptFile, aiTableCfg, isUseRawStructInfo);
        System.out.println(prompt);
        String init = aiCfg.findInit(table);
        System.out.println(init);

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
                    UserMessage.of(prompt),
                    AssistantMessage.of(init),
                    UserMessage.of(ask));
            askWithRetry(messages, retryTimes, stat, tableSchema, openAI,
                    ctx.getContextCfg().dataDir(), aiCfg.model());
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
                              TableSchema tableSchema, SimpleOpenAI openAI,
                              Path dataDir, String model) {
        stat.ask++;
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
                        VTableJsonStore.addOrUpdateRecordStore(record, tableSchema.name(), id, dataDir);
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

}



