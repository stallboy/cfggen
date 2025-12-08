package configgen.geni18n;

import configgen.gen.Parameter;
import configgen.gen.Tool;
import configgen.genbyai.AICfg;
import configgen.geni18n.TermFile.TermEntry;
import configgen.geni18n.TermUpdateModel.Translated;
import configgen.geni18n.TermUpdateWithRefModel;
import configgen.i18n.I18nUtils;
import configgen.i18n.TextByIdFinder.OneText;
import configgen.util.CSVUtil;
import configgen.util.FileUtils;
import configgen.util.JteEngine;
import configgen.util.Logger;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.sashirestela.openai.domain.chat.ChatMessage.*;

/**
 * 术语提取器，使用 AI 从 todo 文件的翻译结果中提取术语，更新术语表文件。
 */
public class TermUpdater extends Tool {
    private final Path termFile;
    private final Path todoFile;
    private final Path referenceTermFile;
    private final String aiCfgFile;
    private final String promptJteFile; // 文件路径或 null（使用默认模板）
    private final int splitChars;
    private AICfg aiCfg;

    public TermUpdater(Parameter parameter) {
        super(parameter);
        String term = parameter.get("term", "term_en.xlsx");
        String todo = parameter.get("todo", "language/_todo_en.xlsx");
        termFile = Path.of(term);
        todoFile = Path.of(todo);

        String referenceTerm = parameter.get("reference", null);
        referenceTermFile = (referenceTerm != null) ? Path.of(referenceTerm) : null;
        aiCfgFile = parameter.get("ai", "ai.json");
        promptJteFile = parameter.get("prompt", null);
        splitChars = Integer.parseInt(parameter.get("maxchars", "96000"));
    }

    @Override
    public void call() {
        if (referenceTermFile == null) {
            updateTermFileWithNoReference();
        } else {
            updateTermFileWithReference();
        }
    }

    public void updateTermFileWithReference() {
        FileUtils.assureFileExistIf(promptJteFile);

        aiCfg = AICfg.readFromFile(aiCfgFile);

        // 1. 读取当前术语文件（只读term部分）
        TermFile termFileObj = TermFile.readTermOnlyInTermFile(termFile);
        List<TermEntry> existingTerms = termFileObj.terms();

        // 2. 读取参考术语文件
        TermFile referenceTermFileObj = TermFile.readTermOnlyInTermFile(referenceTermFile);
        List<TermEntry> referenceTerms = referenceTermFileObj.terms();

        // 3. 收集需要翻译的术语
        Set<String> needTranslateTerms = new HashSet<>();
        for (TermEntry referenceTerm : referenceTerms) {
            boolean found = false;
            for (TermEntry existingTerm : existingTerms) {
                if (existingTerm.original().equals(referenceTerm.original())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                needTranslateTerms.add(referenceTerm.original());
            }
        }

        // 4. 读取todo文件的sources（参考用sheet）
        Map<String, List<OneText>> sourcePairs = readTodoFileSources(todoFile);

        // 5. 精简needTranslateTerms，找到包含关系且长度最小的source pair
        List<OneText> relatedTranslatedPairs = new ArrayList<>();
        Set<String> processedTerms = new HashSet<>();

        for (String term : needTranslateTerms) {
            OneText bestMatch = findBestSourceMatch(term, sourcePairs);
            if (bestMatch != null) {
                relatedTranslatedPairs.add(bestMatch);
                processedTerms.add(term);
            }
        }

        // 6. 构建术语CSV
        String termsInCsv = buildTermsCsv(needTranslateTerms);

        // 7. 构建相关翻译对CSV
        String relatedTranslatedPairsCsv = buildRelatedTranslatedPairsCsv(relatedTranslatedPairs);

        // 8. 准备AI请求数据
        TermUpdateWithRefModel model = new TermUpdateWithRefModel(termsInCsv, relatedTranslatedPairsCsv);

        // 9. 初始化OpenAI客户端
        SimpleOpenAI openAI = SimpleOpenAI.builder()
                .baseUrl(aiCfg.baseUrl())
                .apiKey(aiCfg.apiKey())
                .build();

        // 10. 调用AI
        String prompt = JteEngine.renderTryFileFirst(promptJteFile, "termupdatewithref.jte", model);
        List<TermEntry> newTerms = callAI(prompt, openAI, aiCfg.model());

        // 11. 合并并保存术语表
        List<TermEntry> mergedTerms = TermFile.mergeTerms(existingTerms, newTerms);
        TermFile updatedTermFile = new TermFile(mergedTerms, termFileObj.sources());
        TermFile.writeTermFile(termFile, updatedTermFile);
    }


    public void updateTermFileWithNoReference() {
        FileUtils.assureFileExistIf(promptJteFile);

        aiCfg = AICfg.readFromFile(aiCfgFile);
        // 1. 读取现有术语表
        TermFile termFileObj = TermFile.readTermFile(termFile);

        // 2. 读取 TODO 文件的 "参考用" sheet，按table分组并去重
        var todoEntriesByTable = readTodoFile(todoFile);

        // 3. 准备 AI 请求数据并分批处理
        List<ModelWithSources> batches = prepareBatches(termFileObj, todoEntriesByTable, splitChars);
        Logger.log("split to %d parts", batches.size());

        // 4. 初始化 OpenAI 客户端
        SimpleOpenAI openAI = SimpleOpenAI.builder()
                .baseUrl(aiCfg.baseUrl())
                .apiKey(aiCfg.apiKey())
                .build();

        // 5. 对每个批次调用 AI 并更新术语表
        List<TermEntry> existingTerms = termFileObj.terms();
        for (ModelWithSources batch : batches) {
            // 使用 JTE 模板引擎渲染提示词
            String prompt = JteEngine.renderTryFileFirst(promptJteFile, "termupdate.jte", batch);

            List<TermEntry> newTerms = callAI(prompt, openAI, aiCfg.model());
            existingTerms = TermFile.mergeTerms(existingTerms, newTerms);
            // 每个批次合并后都保存到术语表文件
            TermFile updatedTermFile = new TermFile(existingTerms, batch.newSources());
            TermFile.writeTermFile(termFile, updatedTermFile);
        }
    }

    private Map<String, Map<String, OneText>> readTodoFile(Path todoFile) {
        TodoFile todo = TodoFile.read(todoFile);
        Map<String, Map<String, OneText>> tableMap = new HashMap<>();
        for (TodoFile.Line line : todo.done()) {
            String original = line.original();
            String translated = line.translated();
            if (!original.isEmpty() && !translated.isEmpty()) {
                String key = original + "|" + translated;
                tableMap.computeIfAbsent(line.table(), k -> new HashMap<>())
                        .put(key, new OneText(original, translated));
            }
        }
        return tableMap;
    }


    private record ModelWithSources(TermUpdateModel model,
                                    Map<String, List<OneText>> newSources) {

    }

    private static List<ModelWithSources> prepareBatches(TermFile termFile,
                                                         Map<String, Map<String, OneText>> todoEntriesByTable,
                                                         int maxChars) {
        List<TermEntry> existingTerms = termFile.terms();
        Map<String, List<OneText>> sources = termFile.sources();

        // 构建 existingTerms 的映射：original -> CSV 行，并计算完整术语CSV的总长度
        Map<String, String> termCsvLines = new HashMap<>();
        for (TermEntry term : existingTerms) {
            String escapedOriginal = CSVUtil.escapeCsv(term.original());
            String escapedTranslated = CSVUtil.escapeCsv(term.translated());
            String csvLine = escapedOriginal + "," + escapedTranslated;
            termCsvLines.put(term.original(), csvLine);
        }

        // 处理每个 table，过滤 sources 并准备数据
        List<TableProcessedData> tableDataList = new ArrayList<>();

        for (var entry : todoEntriesByTable.entrySet()) {
            String table = entry.getKey();
            var todoEntries = entry.getValue();

            // 过滤掉已经在 sources 中存在的项
            List<OneText> tableSources = sources.get(table);
            Set<String> existingSourceKeys = new HashSet<>();
            if (tableSources != null) {
                for (OneText source : tableSources) {
                    existingSourceKeys.add(source.original() + "|" + source.translated());
                }
            }

            StringBuilder tableCsv = new StringBuilder();
            List<OneText> newTextsInTable = new ArrayList<>();

            for (var e : todoEntries.entrySet()) {
                String key = e.getKey();
                OneText todo = e.getValue();
                if (existingSourceKeys.contains(key)) {
                    continue;
                }
                String escapedOriginal = CSVUtil.escapeCsv(todo.original());
                String escapedTranslated = CSVUtil.escapeCsv(todo.translated());
                tableCsv.append(escapedOriginal).append(",").append(escapedTranslated).append("\n");
                newTextsInTable.add(todo);
            }

            if (!tableCsv.isEmpty()) {
                tableDataList.add(new TableProcessedData(table, tableCsv.toString(), newTextsInTable));
            }
        }

        // 如果没有任何数据，返回空列表
        if (tableDataList.isEmpty()) {
            // 根据原有逻辑，如果没有 translated 数据，就不生成批次
            return List.of();
        }

        // 分批处理
        List<ModelWithSources> batches = new ArrayList<>();
        List<Translated> currentBatchTrans = new ArrayList<>();
        Set<String> currentBatchOriginals = new HashSet<>();
        Map<String, List<OneText>> currentBatchNewSources = new HashMap<>(sources);
        int currentChars = 0;

        for (TableProcessedData tableData : tableDataList) {
            int tableChars = tableData.csv().length();

            // 如果当前批次不为空且添加此表会超出限制，则创建新批次
            if (!currentBatchTrans.isEmpty() && currentChars + tableChars > maxChars) {
                // 构建当前批次的过滤后术语CSV
                String filteredTermsCsv = buildRelatedTermsCsv(termCsvLines, currentBatchOriginals);
                TermUpdateModel model = new TermUpdateModel(filteredTermsCsv, new ArrayList<>(currentBatchTrans));
                batches.add(new ModelWithSources(model, currentBatchNewSources));

                // 重置当前批次
                currentBatchTrans.clear();
                currentBatchOriginals.clear();
                currentBatchNewSources = new HashMap<>(sources);
                currentChars = 0;
            }

            // 添加当前 table 到批次
            currentBatchTrans.add(new Translated(tableData.table(), tableData.csv()));
            for (OneText newText : tableData.newTexts()) {
                currentBatchOriginals.add(newText.original());
            }

            List<OneText> oneTexts = currentBatchNewSources.get(tableData.table);
            if (oneTexts == null) {
                currentBatchNewSources.put(tableData.table, tableData.newTexts);
            } else {
                oneTexts.addAll(tableData.newTexts);
            }

            currentChars += tableChars;
        }

        // 添加最后一个批次
        String relatedTermsCsv = buildRelatedTermsCsv(termCsvLines, currentBatchOriginals);
        TermUpdateModel model = new TermUpdateModel(relatedTermsCsv, currentBatchTrans);
        batches.add(new ModelWithSources(model, currentBatchNewSources));

        return batches;
    }

    private static String buildRelatedTermsCsv(Map<String, String> termCsvLines, Set<String> originals) {
        if (originals.isEmpty()) {
            return "";
        }
        return termCsvLines.entrySet().stream().parallel()
                .filter(s -> originals.stream().parallel().anyMatch(o -> o.contains(s.getKey())))
                .map(Map.Entry::getValue)
                .collect(Collectors.joining("\n"));
    }

    private record TableProcessedData(String table, String csv, List<OneText> newTexts) {
    }

    private Map<String, List<OneText>> readTodoFileSources(Path todoFile) {
        TodoFile todo = TodoFile.read(todoFile);
        Map<String, List<OneText>> sourceMap = new HashMap<>();
        for (TodoFile.Line line : todo.done()) {
            String original = line.original();
            String translated = line.translated();
            if (!original.isEmpty() && !translated.isEmpty()) {
                sourceMap.computeIfAbsent(line.table(), k -> new ArrayList<>())
                        .add(new OneText(original, translated));
            }
        }
        return sourceMap;
    }

    private OneText findBestSourceMatch(String term, Map<String, List<OneText>> sourcePairs) {
        OneText bestMatch = null;
        int bestLength = Integer.MAX_VALUE;

        // 遍历所有source pairs
        for (List<OneText> pairs : sourcePairs.values()) {
            for (OneText pair : pairs) {
                String original = pair.original();
                // 检查是否包含该术语
                if (original.contains(term)) {
                    // 选择长度最小的匹配
                    if (original.length() < bestLength) {
                        bestLength = original.length();
                        bestMatch = pair;
                    }
                }
            }
        }
        return bestMatch;
    }

    private String buildTermsCsv(Set<String> terms) {
        if (terms.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String term : terms) {
            String escapedTerm = CSVUtil.escapeCsv(term);
            sb.append(escapedTerm).append(",\n"); // 只有原文，翻译为空
        }
        return sb.toString();
    }

    private String buildRelatedTranslatedPairsCsv(List<OneText> pairs) {
        if (pairs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (OneText pair : pairs) {
            String escapedOriginal = CSVUtil.escapeCsv(pair.original());
            String escapedTranslated = CSVUtil.escapeCsv(pair.translated());
            sb.append(escapedOriginal).append(",").append(escapedTranslated).append("\n");
        }
        return sb.toString();
    }


    private static List<TermEntry> callAI(String prompt, SimpleOpenAI openAI, String model) {
        // 构建聊天消息
        List<ChatMessage> messages = List.of(
                UserMessage.of(prompt)
        );

        // 调用 AI
        var chatRequest = ChatRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(0.0)
                .build();
        var futureChat = openAI.chatCompletions().create(chatRequest);
        var chatResponse = futureChat.join();
        String result = chatResponse.firstContent();
        Logger.log("AI 响应:");
        Logger.log(result);
        Logger.log(chatResponse.getUsage().toString());

        // 解析结果中的 Markdown 表格
        return parseMarkdownTable(result);
    }

    static List<TermEntry> parseMarkdownTable(String markdown) {
        List<TermEntry> entries = new ArrayList<>();
        // 简化解析：寻找以 | 开头的行，跳过表头分隔行
        boolean seenHeader = false;
        String[] lines = markdown.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("|") && line.endsWith("|")) {
                // 移除首尾的 |
                String trimmed = line.substring(1, line.length() - 1).trim();
                String[] cells = trimmed.split("\\|");
                // 清理每个单元格的空白
                for (int i = 0; i < cells.length; i++) {
                    cells[i] = cells[i].trim();
                }
                if (cells.length > 1) {
                    // 跳过表头分隔行（通常包含 --- 或 :-- 等）
                    if (cells[0].matches("^[-:]+$")) {
                        seenHeader = true;
                    } else if (seenHeader) {
                        String original = cells[0];
                        String translated = cells[1];
                        if (!original.isBlank() && !translated.isBlank()) {
                            String category = cells.length > 2 ? cells[2] : "";
                            String confidence = cells.length > 3 ? cells[3] : "";
                            String note = cells.length > 4 ? cells[4] : "";
                            // 规范化原始文本
                            String normalized = I18nUtils.normalize(original);
                            entries.add(new TermEntry(normalized, translated, category, confidence, note));
                        }
                    }
                }

            }
        }
        return entries;
    }

}