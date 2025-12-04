package configgen.i18n;

import configgen.gen.Parameter;
import configgen.gen.Tool;
import configgen.genbyai.AICfg;
import configgen.i18n.TermFile.TermEntry;
import configgen.i18n.TermUpdateModel.Translated;
import configgen.i18n.TextByIdFinder.OneText;
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
        aiCfgFile = parameter.get("ai", "ai.json");
        promptJteFile = parameter.get("prompt", null);
        splitChars = Integer.parseInt(parameter.get("maxchars", "96000"));
    }

    @Override
    public void call() {
        aiCfg = AICfg.readFromFile(aiCfgFile);
        // 1. 读取现有术语表
        TermFile termFileObj = TermFile.readTermFile(termFile);

        // 2. 读取 TODO 文件的 "参考用" sheet，按table分组并去重
        var todoEntriesByTable = readTodoFile(todoFile);

        // 3. 准备 AI 请求数据并分批处理
        List<ModelWithSources> batches = prepareBatches(termFileObj, todoEntriesByTable, splitChars);
        Logger.log("split to %d parts", batches.size());

        // 5. 初始化 OpenAI 客户端
        SimpleOpenAI openAI = SimpleOpenAI.builder()
                .baseUrl(aiCfg.baseUrl())
                .apiKey(aiCfg.apiKey())
                .build();

        // 6. 对每个批次调用 AI 并更新术语表
        List<TermEntry> existingTerms = termFileObj.terms();
        Map<String, List<OneText>> sources = termFileObj.sources();

        for (ModelWithSources batch : batches) {
            List<TermEntry> newTerms = callAIForBatch(batch.model(), openAI);
            existingTerms = TermFile.mergeTerms(existingTerms, newTerms);
            // 每个批次合并后都保存到术语表文件
            TermFile updatedTermFile = new TermFile(existingTerms,  batch.newSources());
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


    static String escapeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        // 如果包含逗号、双引号或换行符，用双引号包裹，并且双引号转义为两个双引号
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
            String escapedOriginal = escapeCsv(term.original());
            String escapedTranslated = escapeCsv(term.translated());
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
                String escapedOriginal = escapeCsv(todo.original());
                String escapedTranslated = escapeCsv(todo.translated());
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
                String filteredTermsCsv = buildFilteredTermsCsv(termCsvLines, currentBatchOriginals);
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
        String filteredTermsCsv = buildFilteredTermsCsv(termCsvLines, currentBatchOriginals);
        TermUpdateModel model = new TermUpdateModel(filteredTermsCsv, currentBatchTrans);
        batches.add(new ModelWithSources(model, currentBatchNewSources));

        return batches;
    }

    private static String buildFilteredTermsCsv(Map<String, String> termCsvLines, Set<String> originals) {
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


    private List<TermEntry> callAIForBatch(TermUpdateModel batch,
                                           SimpleOpenAI openAI) {
        // 使用 JTE 模板引擎渲染提示词
        String prompt = JteEngine.renderTryFileFirst(promptJteFile, "termupdate.jte", batch);

        // 构建聊天消息
        List<ChatMessage> messages = List.of(
                UserMessage.of(prompt)
        );

        // 调用 AI
        var chatRequest = ChatRequest.builder()
                .model(aiCfg.model())
                .messages(messages)
                .temperature(0.0)
                .build();
        var futureChat = openAI.chatCompletions().create(chatRequest);
        var chatResponse = futureChat.join();
        String result = chatResponse.firstContent();
        System.out.println("AI 响应:");
        System.out.println(result);
        System.out.println(chatResponse.getUsage().toString());

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
                            String normalized = Utils.normalize(original);
                            entries.add(new TermEntry(normalized, translated, category, confidence, note));
                        }
                    }
                }

            }
        }
        return entries;
    }

}