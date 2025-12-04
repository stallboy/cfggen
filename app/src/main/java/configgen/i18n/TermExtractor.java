package configgen.i18n;

import configgen.gen.Parameter;
import configgen.gen.Tool;
import configgen.genbyai.AICfg;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static io.github.sashirestela.openai.domain.chat.ChatMessage.*;

/**
 * 术语提取器，使用 AI 从 todo 文件的翻译结果中提取术语，更新术语表文件。
 */
public class TermExtractor extends Tool {
    private final Path termFile;
    private final Path todoFile;
    private final String aiCfgFile;
    private final String promptJteFile; // 文件路径或 null（使用默认模板）
    private AICfg aiCfg;

    public TermExtractor(Parameter parameter) {
        super(parameter);
        String term = parameter.get("term", "term_en.xlsx");
        String todo = parameter.get("todo", "language/_todo_en.xlsx");
        termFile = Path.of(term);
        todoFile = Path.of(todo);
        aiCfgFile = parameter.get("ai", "ai.json");
        promptJteFile = parameter.get("prompt", null);

    }

    @Override
    public void call() {
        try {
            update();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行术语提取和更新
     */
    public void update() throws IOException {
        aiCfg = AICfg.readFromFile(aiCfgFile);
        // 1. 读取现有术语表
        List<TermEntry> existingTerms = readTermFile(termFile);

        // 2. 读取 TODO 文件的 "参考用" sheet
        List<TodoEntry> todoEntries = readTodoFile(todoFile);

        // 3. 去重整理数据
        List<TodoEntry> uniqueEntries = deduplicateTodoEntries(todoEntries);

        // 4. 准备 AI 请求数据
        TermUpdateModel updateModel = prepareUpdateModel(existingTerms, uniqueEntries);

        // 5. 如果数据量太大，分批处理
        List<TermUpdateModel> batches = splitIntoBatches(updateModel, 12000);

        // 6. 初始化 OpenAI 客户端
        SimpleOpenAI openAI = SimpleOpenAI.builder()
                .baseUrl(aiCfg.baseUrl())
                .apiKey(aiCfg.apiKey())
                .build();

        // 7. 对每个批次调用 AI 并更新术语表
        for (TermUpdateModel batch : batches) {
            List<TermEntry> newTerms = callAIForBatch(batch, openAI);
            existingTerms = mergeTerms(existingTerms, newTerms);
        }

        // 9. 写回术语表文件
        writeTermFile(termFile, existingTerms);
    }


    // 内部数据记录类
    record TermEntry(String original, String translated, String category, String note) {
    }

    record TodoEntry(String table, String original, String translated) {
    }

    record Translated(String table, String translatedInCsv) {
    }

    record TermUpdateModel(String termsInCsv, List<Translated> tableTranslatedList) {
    }

    // 以下为具体实现方法，将在后续步骤中实现

    private List<TermEntry> readTermFile(Path termFile) {
        try (ReadableWorkbook wb = new ReadableWorkbook(termFile.toFile(), new ReadingOptions(true, false))) {
            for (Sheet sheet : wb.getSheets().toList()) {
                List<Row> rows = sheet.read();
                List<TermEntry> result = new ArrayList<>(rows.size());
                for (Row row : rows) {
                    String original = row.getCellAsString(0).orElse("");
                    String translated = row.getCellAsString(1).orElse("");
                    String category = row.getCellAsString(2).orElse("");
                    String note = row.getCellAsString(3).orElse("");
                    // 规范化原始文本（与 TermChecker 保持一致）
                    String normalized = Utils.normalize(original);
                    if (!original.isEmpty() && !translated.isEmpty()) {
                        result.add(new TermEntry(normalized, translated, category, note));
                    }
                }
                return result;
            }
        } catch (IOException e) {
            throw new RuntimeException("读取术语文件失败: " + termFile, e);
        }
        return Collections.emptyList();
    }

    private List<TodoEntry> readTodoFile(Path todoFile) {
        try (ReadableWorkbook wb = new ReadableWorkbook(todoFile.toFile(), new ReadingOptions(true, false))) {
            Optional<Sheet> doneSheet = wb.findSheet("参考用");
            if (doneSheet.isEmpty()) {
                throw new RuntimeException("TODO 文件中未找到 '参考用' sheet: " + todoFile);
            }
            List<Row> rows = doneSheet.get().read();
            List<TodoEntry> entries = new ArrayList<>(rows.size());
            // 跳过表头行（第一行）
            for (int i = 1; i < rows.size(); i++) {
                Row row = rows.get(i);
                String table = row.getCellAsString(0).orElse("");
                // id 和 fieldChain 不需要
                String original = row.getCellAsString(3).orElse("");
                String translated = row.getCellAsString(4).orElse("");
                // 规范化原始文本
                String normalized = Utils.normalize(original);
                if (!original.isEmpty() && !translated.isEmpty()) {
                    entries.add(new TodoEntry(table, normalized, translated));
                }
            }
            return entries;
        } catch (IOException e) {
            throw new RuntimeException("读取 TODO 文件失败: " + todoFile, e);
        }
    }

    private List<TodoEntry> deduplicateTodoEntries(List<TodoEntry> entries) {
        // 按 table 分组，然后在每个组内根据 (original, translated) 去重
        Map<String, Set<String>> tableSeen = new HashMap<>();
        List<TodoEntry> result = new ArrayList<>();
        for (TodoEntry entry : entries) {
            String key = entry.original() + "|" + entry.translated();
            Set<String> seen = tableSeen.computeIfAbsent(entry.table(), k -> new HashSet<>());
            if (!seen.contains(key)) {
                seen.add(key);
                result.add(entry);
            }
        }
        return result;
    }

    private TermUpdateModel prepareUpdateModel(List<TermEntry> existingTerms, List<TodoEntry> todoEntries) {
        // 将现有术语转换为 CSV 格式：original,translated,category,note
        StringBuilder termsCsv = new StringBuilder();
        for (TermEntry term : existingTerms) {
            // 转义 CSV 特殊字符（简化处理，使用引号包裹）
            String escapedOriginal = escapeCsv(term.original());
            String escapedTranslated = escapeCsv(term.translated());
            String escapedCategory = escapeCsv(term.category());
            String escapedNote = escapeCsv(term.note());
            termsCsv.append(escapedOriginal).append(",")
                    .append(escapedTranslated).append(",")
                    .append(escapedCategory).append(",")
                    .append(escapedNote).append("\n");
        }

        // 按 table 分组 todo 条目
        Map<String, List<TodoEntry>> groupedByTable = new HashMap<>();
        for (TodoEntry entry : todoEntries) {
            groupedByTable.computeIfAbsent(entry.table(), k -> new ArrayList<>()).add(entry);
        }

        // 为每个 table 生成 Translated 记录
        List<Translated> translatedList = new ArrayList<>();
        for (Map.Entry<String, List<TodoEntry>> entry : groupedByTable.entrySet()) {
            String table = entry.getKey();
            StringBuilder tableCsv = new StringBuilder();
            for (TodoEntry todo : entry.getValue()) {
                String escapedOriginal = escapeCsv(todo.original());
                String escapedTranslated = escapeCsv(todo.translated());
                tableCsv.append(escapedOriginal).append(",").append(escapedTranslated).append("\n");
            }
            translatedList.add(new Translated(table, tableCsv.toString()));
        }

        return new TermUpdateModel(termsCsv.toString(), translatedList);
    }

    private String escapeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        // 如果包含逗号、双引号或换行符，用双引号包裹，并且双引号转义为两个双引号
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private List<TermUpdateModel> splitIntoBatches(TermUpdateModel updateModel, int maxChars) {
        // 计算总字符数
        int totalChars = updateModel.termsInCsv().length();
        for (Translated trans : updateModel.tableTranslatedList()) {
            totalChars += trans.translatedInCsv().length();
        }

        // 如果总字符数不超过限制，返回单个批次
        if (totalChars <= maxChars) {
            return List.of(updateModel);
        }

        // 否则按 table 拆分
        List<TermUpdateModel> batches = new ArrayList<>();
        List<Translated> currentBatchTrans = new ArrayList<>();
        int currentChars = updateModel.termsInCsv().length();

        for (Translated trans : updateModel.tableTranslatedList()) {
            int transChars = trans.translatedInCsv().length();
            // 如果当前批次不为空且添加此表会超出限制，则创建新批次
            if (!currentBatchTrans.isEmpty() && currentChars + transChars > maxChars) {
                batches.add(new TermUpdateModel(updateModel.termsInCsv(), new ArrayList<>(currentBatchTrans)));
                currentBatchTrans.clear();
                currentChars = updateModel.termsInCsv().length();
            }
            currentBatchTrans.add(trans);
            currentChars += transChars;
        }

        // 添加最后一个批次
        if (!currentBatchTrans.isEmpty()) {
            batches.add(new TermUpdateModel(updateModel.termsInCsv(), currentBatchTrans));
        }

        return batches;
    }

    private String readPromptTemplate() throws IOException {
        // 如果提供了文件路径，从文件系统读取；否则从 classpath 读取默认模板
        if (promptJteFile != null && !promptJteFile.isEmpty()) {
            return Files.readString(Path.of(promptJteFile));
        } else {
            // 从 classpath 读取默认模板
            ClassLoader classLoader = TermExtractor.class.getClassLoader();
            try (java.io.InputStream is = classLoader.getResourceAsStream("jte/term_extractor_prompt.jte")) {
                if (is == null) {
                    throw new RuntimeException("默认提示词模板未找到：jte/term_extractor_prompt.jte");
                }
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
    }

    private List<TermEntry> callAIForBatch(TermUpdateModel batch,
                                           SimpleOpenAI openAI) {
        // 使用 JTE 模板引擎渲染提示词
        gg.jte.output.StringOutput output = new gg.jte.output.StringOutput();
        configgen.util.JteEngine.render("term_extractor_prompt.jte", batch, output);
        String renderedPrompt = output.toString();

        // 构建聊天消息
        List<ChatMessage> messages = List.of(
                UserMessage.of(renderedPrompt)
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

    private List<TermEntry> parseMarkdownTable(String markdown) {
        List<TermEntry> entries = new ArrayList<>();
        // 简化解析：寻找以 | 开头的行，跳过表头分隔行
        String[] lines = markdown.split("\n");
        boolean inTable = false;
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
                // 跳过表头分隔行（通常包含 --- 或 :-- 等）
                if (cells.length >= 4 && !cells[0].matches("^[-:]+$")) {
                    String original = cells[0];
                    String translated = cells[1];
                    String category = cells[2];
                    String note = cells[3];
                    // 规范化原始文本
                    String normalized = Utils.normalize(original);
                    entries.add(new TermEntry(normalized, translated, category, note));
                }
            }
        }
        return entries;
    }

    private List<TermEntry> mergeTerms(List<TermEntry> existing, List<TermEntry> newTerms) {
        // 按 original 建立映射
        Map<String, TermEntry> map = new LinkedHashMap<>();
        for (TermEntry term : existing) {
            map.put(term.original(), term);
        }
        // 合并新术语
        for (TermEntry newTerm : newTerms) {
            TermEntry existingTerm = map.get(newTerm.original());
            if (existingTerm == null) {
                map.put(newTerm.original(), newTerm);
            } else {
                // 更新翻译、类别和备注（如果新术语提供了非空值）
                String translated = newTerm.translated().isEmpty() ? existingTerm.translated() : newTerm.translated();
                String category = newTerm.category().isEmpty() ? existingTerm.category() : newTerm.category();
                String note = newTerm.note().isEmpty() ? existingTerm.note() : newTerm.note();
                map.put(newTerm.original(), new TermEntry(newTerm.original(), translated, category, note));
            }
        }
        return new ArrayList<>(map.values());
    }

    private void writeTermFile(Path termFile, List<TermEntry> terms) throws IOException {
        try (java.io.OutputStream os = new java.io.BufferedOutputStream(
                new java.io.FileOutputStream(termFile.toFile()))) {
            try (Workbook wb = new Workbook(os, "term", "1.0")) {
                Worksheet ws = wb.newWorksheet("Sheet1");
                // 写入表头
                ws.inlineString(0, 0, "original");
                ws.inlineString(0, 1, "translated");
                ws.inlineString(0, 2, "category");
                ws.inlineString(0, 3, "note");
                // 写入数据行
                for (int i = 0; i < terms.size(); i++) {
                    TermEntry term = terms.get(i);
                    ws.inlineString(i + 1, 0, term.original());
                    ws.inlineString(i + 1, 1, term.translated());
                    ws.inlineString(i + 1, 2, term.category());
                    ws.inlineString(i + 1, 3, term.note());
                }
            }
        }
    }
}