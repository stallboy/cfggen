package configgen.genbyai;

import configgen.util.CachedFiles;

import org.jspecify.annotations.Nullable;

import configgen.ctx.Context;
import configgen.data.CfgData;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.schema.*;
import configgen.value.*;
import configgen.write.VTableJsonStorage;
import configgen.write.VTableStorage;
import configgen.util.ChatCompletionsClient;
import configgen.util.ChatCompletionsClient.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ByAIGenerator extends Generator {
    private final String cfgFn;
    private final String askFn;
    private final String table;
    private final int retryTimes;

    public ByAIGenerator(Parameter parameter) {
        super(parameter);
        cfgFn = parameter.get("cfg", "ai.json");
        askFn = parameter.get("ask", "ask.txt");
        table = parameter.get("table", "skill.buff");
        retryTimes = Integer.parseInt(parameter.get("retry", "1"));
    }


    @Override
    public void generate(Context ctx, CachedFiles outputFiles) throws IOException {
        if (retryTimes <= 0) {
            throw new RuntimeException("retry must > 0");
        }

        var aiCfg = ChatCompletionsClient.Config.readFromFile(cfgFn);
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
        ChatCompletionsClient client = new ChatCompletionsClient(aiCfg);

        AskStat stat = new AskStat();
        for (String ask : asks) {
            if (ask.trim().isEmpty()) {
                continue;
            }

            System.out.println();
            System.out.printf("## %s%n", ask);
            List<Message> messages = List.of(
                    new Message("user", prompt.prompt()),
                    new Message("assistant", prompt.init()),
                    new Message("user", ask));
            askWithRetry(messages, retryTimes, stat, ctx, vTable, client);
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

    private void askWithRetry(List<Message> messages, int retryTimes, AskStat stat,
                              Context context, CfgValue.VTable vTable,
                              ChatCompletionsClient client) {
        stat.ask++;
        TableSchema tableSchema = vTable.schema();
        for (int i = 0; i < retryTimes; i++) {
            String jsonResult = ask(messages, client);
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
                                    context.rootDir(), context.sourceStructure());
                        } else {
                            CfgData.DTable dTable = context.cfgData().getDTable(table);
                            VTableStorage.addOrUpdateRecord(context, vTable, java.util.Objects.requireNonNull(dTable), pkValue, record);
                        }

                    } catch (IOException e) {
                        System.out.printf("save %s err: %s%n", id, e.getMessage());
                    }
                    return;
                } else {
                    messages = new ArrayList<>(messages);
                    messages.add(new Message("user", PromptDefault.FIX_ERROR.formatted(parseErrs.toString())));

                    stat.err++;
                }
            }
        }
    }

    private @Nullable String ask(List<Message> messages, ChatCompletionsClient client) {
        var completion = client.complete(messages);
        String result = completion.content();
        System.out.println(result);
        if (completion.usage() != null) {
            System.out.println(completion.usage());
        }
        return extractJson(result);
    }

    private static final Pattern pattern = Pattern.compile("```json\\s*([^`]+)\\s*```");

    public static @Nullable String extractJson(String input) {
        Matcher matcher = pattern.matcher(input);
        String lastJson = null;
        while (matcher.find()) {
            lastJson = matcher.group(1).trim();
        }
        return lastJson;
    }

}



