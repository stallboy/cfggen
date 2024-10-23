package configgen.tool;

import com.alibaba.fastjson2.JSON;
import configgen.ctx.Context;
import configgen.data.Source;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.schema.*;
import configgen.schema.cfg.CfgWriter;
import configgen.value.*;
import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;
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

public class GenJsonByAI extends Generator {
    public record OneExample(
            String id,
            String description) {
    }

    public record TableCfg(
            String table,
            List<String> extraRefTables,
            List<OneExample> examples) {
    }

    public record AICfg(
            String baseUrl,
            String apiKey,
            String model,
            List<TableCfg> tableCfgs) {

        public TableCfg findTable(String table) {
            for (TableCfg tableCfg : tableCfgs) {
                if (tableCfg.table.equals(table)) {
                    return tableCfg;
                }
            }
            return null;
        }
    }

    public record Example(
            String id,
            String description,
            String json) {
    }

    public record PromptModel(
            String structInfo,
            String extra,
            List<Example> examples) {
    }

    private static final String ANSWER = "请提供ID和描述，我将根据这些信息生成符合结构的JSON配置。";

    private final AICfg aiCfg;
    private final List<String> asks;
    private final String table;
    private final String promptFn;
    private final int retryTimes;
    private final boolean isUseRawStructInfo;

    public GenJsonByAI(Parameter parameter) {
        super(parameter);
        String cfgFn = parameter.get("cfg", "ai.json");
        String askFn = parameter.get("ask", "ask.txt");
        table = parameter.get("table", "skill.buff");
        String pn = parameter.get("promptfn", null);
        if (pn == null) {
            pn = table + ".jte";
        }
        promptFn = pn;
        isUseRawStructInfo = parameter.has("raw");
        retryTimes = Integer.parseInt(parameter.get("retry", "1"));

        if (retryTimes <= 0) {
            throw new RuntimeException("retry must > 0");
        }
        if (tag != null) {
            throw new RuntimeException("gen jsonByAI with tag=%s not supported".formatted(tag));
        }
        if (!Files.exists(Path.of(cfgFn))) {
            throw new RuntimeException(cfgFn + " not exist!");
        }
        if (!Files.exists(Path.of(askFn))) {
            throw new RuntimeException(askFn + " not exist!");
        }
        if (!Files.exists(Path.of(promptFn))) {
            throw new RuntimeException(promptFn + " not exist!");
        }

        String jsonStr;
        try {
            jsonStr = Files.readString(Path.of(cfgFn));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        aiCfg = JSON.parseObject(jsonStr, AICfg.class);

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

        TableSchema tableSchema = vTable.schema();

        // 创建model
        String structInfo;
        String extra;
        TableCfg tableCfg = aiCfg.findTable(table);
        List<String> extraRefTables = tableCfg != null ? tableCfg.extraRefTables : List.of();
        List<OneExample> examples = tableCfg != null ? tableCfg.examples : List.of();
        if (isUseRawStructInfo) {
            structInfo = getTableSchemaRelatedCfgStr(tableSchema);
            extra = getTableRelatedEnumCsv(cfgValue, tableSchema, extraRefTables);
        } else {
            structInfo = new SchemaToTs(cfgValue, tableSchema, extraRefTables, true).generate();
            extra = "";
        }

        PromptModel model = new PromptModel(structInfo, extra, getExamples(examples, vTable));


        // 生成prompt
        CodeResolver codeResolver = new DirectoryCodeResolver(Path.of("."));
        TemplateEngine templateEngine = TemplateEngine.create(codeResolver, ContentType.Plain);
        TemplateOutput prompt = new StringOutput();
        templateEngine.render(promptFn, model, prompt);

        String promptStr = prompt.toString();
        System.out.println(promptStr);

        // 调用llm
        SimpleOpenAI openAI = SimpleOpenAI.builder()
                .baseUrl(aiCfg.baseUrl)
                .apiKey(aiCfg.apiKey)
                .build();

        AskStat stat = new AskStat();
        for (String ask : asks) {
            if (ask.trim().isEmpty()) {
                continue;
            }

            System.out.println();
            System.out.printf("## %s%n", ask);
            List<ChatMessage> messages = List.of(
                    UserMessage.of(promptStr),
                    AssistantMessage.of(ANSWER),
                    UserMessage.of(ask));
            askWithRetry(messages, retryTimes, stat, tableSchema, openAI,
                    ctx.dataDir(), cfgValue.valueStat());
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
                              Path dataDir, ValueStat valueStat) {

        stat.ask++;
        for (int i = 0; i < retryTimes; i++) {
            String jsonResult = ask(messages, openAI);
            if (i > 0) {
                stat.retry++;
            }

            if (jsonResult == null) {
                stat.noJson++;
                return;
            } else {
                ValueErrs parseErrs = ValueErrs.of();
                CfgValue.VStruct record = new ValueJsonParser(tableSchema, parseErrs).fromJson(jsonResult, Source.DFile.of("<server>", table));
                parseErrs.checkErrors("check json", true, true);

                if (!parseErrs.warns().isEmpty()) {
                    stat.warn++;
                }
                if (parseErrs.errs().isEmpty()) {
                    stat.ok++;

                    CfgValue.Value pkValue = ValueUtil.extractPrimaryKeyValue(record, tableSchema);
                    String id = pkValue.packStr();
                    try {
                        VTableJsonStore.addOrUpdateRecordStore(record, tableSchema, id, dataDir, valueStat);
                    } catch (IOException e) {
                        System.out.printf("save %s err: %s%n", id, e.getMessage());
                    }
                    return;
                } else {
                    messages = new ArrayList<>(messages);
                    messages.add(UserMessage.of("json不符合结构定义，错误如下：%s，请改正".formatted(parseErrs.toString())));

                    stat.err++;
                }
            }
        }

    }


    private String ask(List<ChatMessage> messages, SimpleOpenAI openAI) {
        var chatRequest = ChatRequest.builder()
                .model(aiCfg.model)
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

    static String extractJson(String input) {
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    static String getTableSchemaRelatedCfgStr(TableSchema tableSchema) {
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

    static String getTableRelatedEnumCsv(CfgValue cfgValue, TableSchema tableSchema, List<String> extraRefTables) {
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


    static void addOneRelatedCsv(StringBuilder sb, CfgValue cfgValue, TableSchema tableSchema, EntryType.EEnum en) {
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

    static List<Example> getExamples(List<OneExample> rawExamples, CfgValue.VTable vTable) {
        List<Example> examples = new ArrayList<>(rawExamples.size());
        for (OneExample ex : rawExamples) {
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
        return examples;
    }
}



