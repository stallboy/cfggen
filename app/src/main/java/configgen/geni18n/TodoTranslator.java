package configgen.geni18n;

import configgen.gen.Parameter;
import configgen.gen.Tool;
import configgen.genbyai.AICfg;
import configgen.genbyai.GenByAI;
import configgen.geni18n.TodoEdit.AITranslationEntry;
import configgen.geni18n.TodoEdit.AITranslationResult;
import configgen.geni18n.TodoEdit.DoneByTable;
import configgen.geni18n.TodoEdit.TodoOriginalsByTable;
import configgen.i18n.TextByIdFinder.OneText;
import configgen.util.CSVUtil;
import configgen.util.FileUtils;
import configgen.util.JteEngine;
import configgen.util.Logger;
import configgen.i18n.I18nUtils;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import org.apache.commons.text.similarity.LevenshteinDistance;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.nio.file.Path;
import java.util.*;

import static io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;

/**
 * 使用AI翻译TODO文件中的待翻译文本。
 */
public class TodoTranslator extends Tool {
    private final String sourceLang;
    private final String targetLang;

    private final String termFile;
    private final String todoFile;
    private final String aiCfgFile;
    private final String promptJteFile; // 文件路径或null（使用默认模板）

    private final int maxLines;
    private AICfg aiCfg;

    public TodoTranslator(Parameter parameter) {
        super(parameter);
        sourceLang = parameter.get("source", "中文");
        targetLang = parameter.get("target", "英文");

        todoFile = parameter.get("todo", "language/_todo_en.xlsx");
        termFile = parameter.get("term", null);
        aiCfgFile = parameter.get("ai", "ai.json");
        promptJteFile = parameter.get("prompt", null);
        maxLines = Integer.parseInt(parameter.get("maxlines", "30"));
    }

    @Override
    public void call() {
        FileUtils.assureFileExistIf(promptJteFile);

        aiCfg = AICfg.readFromFile(aiCfgFile);
        Logger.log("开始翻译TODO文件: %s", todoFile);

        List<OneText> terms = null;
        if (termFile != null) {
            terms = TermFile.loadTerm(Path.of(termFile));
        }


        // 1. 读取todo文件
        Path todoFilePath = Path.of(todoFile);
        TodoEdit todoEdit = TodoEdit.read(todoFilePath);

        // 2. 构建done sheet的映射：table -> (original -> translated)
        DoneByTable doneByTable = todoEdit.parseDoneByTable();

        // 3. 收集需要翻译的todo行
        TodoOriginalsByTable todoOriginalsByTable = todoEdit.useTranslationsInDoneIfSameOriginal(doneByTable);

        // 检查是否有需要翻译的文本
        if (todoOriginalsByTable.isEmpty()) {
            Logger.log("没有需要翻译的文本");
            return;
        }

        // 4. 按table为单位进行分批处理
        List<List<TableOriginal>> batches = splitIntoBatchesByTable(todoOriginalsByTable, maxLines);


        // 5. 初始化OpenAI客户端
        SimpleOpenAI openAI = SimpleOpenAI.builder()
                .baseUrl(aiCfg.baseUrl())
                .apiKey(aiCfg.apiKey())
                .build();

        // 6. 处理每个批次
        for (List<TableOriginal> batch : batches) {
            // 收集批次中所有table的待翻译文本
            Set<String> todoOriginals = new LinkedHashSet<>();
            Map<String, String> mostSimilarResult = new HashMap<>();

            for (TableOriginal to : batch) {
                todoOriginals.add(to.original);
                Map.Entry<String, String> bestMatch = findMostSimilarTranslation(to.original, doneByTable.get(to.table));
                if (bestMatch != null) {
                    // 只添加不重复的翻译
                    mostSimilarResult.put(bestMatch.getKey(), bestMatch.getValue());
                }
            }

            if (todoOriginals.isEmpty()) {
                continue;
            }

            // 构建相关术语CSV
            String relatedTermsCsv = buildRelatedTermsCsv(todoOriginals, terms);

            // 获取合并后的相关翻译CSV
            String relatedTranslationsCsv = buildRelatedTranslationsCsv(mostSimilarResult);

            // 创建TranslateModel
            TodoTranslateModel model = new TodoTranslateModel(sourceLang, targetLang,
                    todoOriginals.stream().toList(),
                    relatedTermsCsv,
                    relatedTranslationsCsv);
            // 渲染提示词
            String prompt = JteEngine.renderTryFileFirst(promptJteFile, "translate.jte", model);
            // 调用AI
            AITranslationResult aiResult = callAI(prompt, openAI);

            todoEdit.useAITranslationResult(aiResult);
            // 更新todo sheet
            todoEdit.save(todoFilePath);
        }

        Logger.log("翻译完成");
    }

    private record TableOriginal(String table,
                                 String original) {
    }

    private List<List<TableOriginal>> splitIntoBatchesByTable(TodoOriginalsByTable todoOriginalsByTable,
                                                              int maxBatchSize) {
        // 1. 先展开成List，每个item是(table, original)
        List<TableOriginal> allItems = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : todoOriginalsByTable.entrySet()) {
            String table = entry.getKey();
            for (String original : entry.getValue()) {
                allItems.add(new TableOriginal(table, original));
            }
        }

        // 2. 按maxBatchSize拆分
        List<List<TableOriginal>> batches = new ArrayList<>();
        for (int i = 0; i < allItems.size(); i += maxBatchSize) {
            int end = Math.min(i + maxBatchSize, allItems.size());
            List<TableOriginal> batch = allItems.subList(i, end);
            batches.add(batch);
        }

        Logger.log("allItems = %d, split to %d parts", allItems.size(), batches.size());
        return batches;
    }


    private AITranslationResult callAI(String prompt, SimpleOpenAI openAI) {
        Logger.log("AI请求:\n%s", prompt);
        List<ChatMessage> messages = List.of(UserMessage.of(prompt));
        var chatRequest = ChatRequest.builder()
                .model(aiCfg.model())
                .messages(messages)
                .temperature(0.0)
                .build();
        var futureChat = openAI.chatCompletions().create(chatRequest);
        var chatResponse = futureChat.join();
        String result = chatResponse.firstContent();
        Logger.log("AI响应:\n%s", result);
        Logger.log(chatResponse.getUsage().toString());
        // 解析结果
        return parseAIResult(result);
    }


    private static AITranslationResult parseAIResult(String response) {
        AITranslationResult entries = new AITranslationResult();

        String jsonStr = GenByAI.extractJson(response);
        if (jsonStr == null) {
            jsonStr = response;
        }

        // 尝试解析JSON数组
        JSONArray jsonArray = JSON.parseArray(jsonStr);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);
            String original = item.getString("original");
            String translated = item.getString("translated");
            String confidence = item.getString("confidence");
            String note = item.getString("note");

            if (original != null && !original.isBlank() &&
                    translated != null && !translated.isBlank()) {
                // 规范化原始文本
                String normalized = I18nUtils.normalize(original);
                entries.put(normalized, new AITranslationEntry(translated, confidence, note));
            }
        }
        return entries;
    }

    private static String buildRelatedTermsCsv(Set<String> todoOriginals, List<OneText> terms) {
        if (terms == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (OneText term : terms) {
            // 检查术语是否出现在任何待翻译文本中
            boolean found = false;
            for (String original : todoOriginals) {
                if (original.contains(term.original())) {
                    found = true;
                    break;
                }
            }
            if (found) {
                sb.append(CSVUtil.escapeCsv(term.original())).append(",");
                sb.append(CSVUtil.escapeCsv(term.translated())).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 构建单个table的相关翻译CSV
     */
    private static String buildRelatedTranslationsCsv(Map<String, String> mostSimilarResult) {
        // 将Map转换为CSV
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : mostSimilarResult.entrySet()) {
            sb.append(CSVUtil.escapeCsv(entry.getKey())).append(",");
            sb.append(CSVUtil.escapeCsv(entry.getValue())).append("\n");
        }

        return sb.toString();
    }

    /**
     * 查找最相似的一个翻译
     */
    private static Map.Entry<String, String> findMostSimilarTranslation(String todoOriginal,
                                                                        Map<String, String> doneTranslations) {
        if (doneTranslations == null || doneTranslations.isEmpty()) {
            return null;
        }

        Map.Entry<String, String> bestMatch = null;
        double bestSimilarity = -1.0;

        for (Map.Entry<String, String> entry : doneTranslations.entrySet()) {
            double similarity = calculateSimilarity(todoOriginal, entry.getKey());
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = entry;
            }
        }


        if (bestSimilarity > 0.5) {
            return bestMatch;
        }

        return null;

    }

    /**
     * 计算两个字符串的相似度（使用Levenshtein距离）
     * 相似度 = 1 - (编辑距离 / max(len1, len2))
     */
    private static double calculateSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null || str1.isEmpty() || str2.isEmpty()) {
            return 0.0;
        }


        // 使用Apache Commons Text的LevenshteinDistance
        LevenshteinDistance levenshtein = LevenshteinDistance.getDefaultInstance();
        Integer distance = levenshtein.apply(str1, str2);

        if (distance == null) {
            return 0.0;
        }

        int len1 = str1.length();
        int len2 = str2.length();
        int maxLen = Math.max(len1, len2);

        // 计算相似度：1 - (距离/最大长度)
        //noinspection UnnecessaryLocalVariable
        double res = 1.0 - ((double) distance / maxLen);
//        System.out.printf("%s ----- %s ----- %f\n", str1, str2, res);
        return res;
    }

}