package configgen.i18n;

import configgen.gen.Parameter;
import configgen.gen.Tool;
import configgen.genbyai.AICfg;
import configgen.util.JteEngine;
import configgen.util.Logger;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

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
        List<TermFile.TermEntry> existingTerms = TermFile.readTermFile(termFile);

        // 2. 读取 TODO 文件的 "参考用" sheet，按table分组并去重
        Map<String, List<TodoEntry>> todoEntriesByTable = readTodoFile(todoFile);

        // 3. 准备 AI 请求数据
        TermUpdateModel updateModel = prepareUpdateModel(existingTerms, todoEntriesByTable);

        // 4. 如果数据量太大，分批处理
        List<TermUpdateModel> batches = splitIntoBatches(updateModel, splitChars);
        Logger.log("split to %d parts", batches.size());

        // 5. 初始化 OpenAI 客户端
        SimpleOpenAI openAI = SimpleOpenAI.builder()
                .baseUrl(aiCfg.baseUrl())
                .apiKey(aiCfg.apiKey())
                .build();

        // 6. 对每个批次调用 AI 并更新术语表
        for (TermUpdateModel batch : batches) {
            List<TermFile.TermEntry> newTerms = callAIForBatch(batch, openAI);
            existingTerms = TermFile.mergeTerms(existingTerms, newTerms);
        }

        // 7. 写回术语表文件
        TermFile.writeTermFile(termFile, existingTerms);
    }


    record TodoEntry(String original,
                     String translated) {
    }


    private Map<String, List<TodoEntry>> readTodoFile(Path todoFile) {
        try (ReadableWorkbook wb = new ReadableWorkbook(todoFile.toFile(), new ReadingOptions(true, false))) {
            Optional<Sheet> doneSheet = wb.findSheet("参考用");
            if (doneSheet.isEmpty()) {
                throw new RuntimeException("TODO 文件中未找到 '参考用' sheet: " + todoFile);
            }
            List<Row> rows = doneSheet.get().read();
            // 使用Map按table分组，每个table内用Set去重（基于original+translated）
            Map<String, Map<String, TodoEntry>> tableMap = new HashMap<>();
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
                    TodoEntry entry = new TodoEntry(normalized, translated);
                    String key = normalized + "|" + translated;
                    tableMap.computeIfAbsent(table, k -> new HashMap<>()).put(key, entry);
                }
            }
            // 转换为Map<String, List<TodoEntry>>
            Map<String, List<TodoEntry>> result = new HashMap<>();
            for (Map.Entry<String, Map<String, TodoEntry>> entry : tableMap.entrySet()) {
                result.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("读取 TODO 文件失败: " + todoFile, e);
        }
    }


    private static TermUpdateModel prepareUpdateModel(List<TermFile.TermEntry> existingTerms, Map<String, List<TodoEntry>> todoEntriesByTable) {
        // 将现有术语转换为 CSV 格式：original,translated,category,note
        StringBuilder termsCsv = new StringBuilder();
        for (TermFile.TermEntry term : existingTerms) {
            // 转义 CSV 特殊字符（简化处理，使用引号包裹）
            String escapedOriginal = escapeCsv(term.original());
            String escapedTranslated = escapeCsv(term.translated());
            termsCsv.append(escapedOriginal).append(",")
                    .append(escapedTranslated).append("\n");
        }

        // 为每个 table 生成 Translated 记录
        List<TermUpdateModel.Translated> translatedList = new ArrayList<>();
        for (Map.Entry<String, List<TodoEntry>> entry : todoEntriesByTable.entrySet()) {
            String table = entry.getKey();
            StringBuilder tableCsv = new StringBuilder();
            for (TodoEntry todo : entry.getValue()) {
                String escapedOriginal = escapeCsv(todo.original());
                String escapedTranslated = escapeCsv(todo.translated());
                tableCsv.append(escapedOriginal).append(",").append(escapedTranslated).append("\n");
            }
            translatedList.add(new TermUpdateModel.Translated(table, tableCsv.toString()));
        }

        return new TermUpdateModel(termsCsv.toString(), translatedList);
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

    private static List<TermUpdateModel> splitIntoBatches(TermUpdateModel updateModel, int maxChars) {
        // 计算总字符数
        int totalChars = updateModel.termsInCsv().length();
        for (TermUpdateModel.Translated trans : updateModel.tableTranslatedList()) {
            totalChars += trans.translatedInCsv().length();
        }

        // 如果总字符数不超过限制，返回单个批次
        if (totalChars <= maxChars) {
            return List.of(updateModel);
        }

        // 否则按 table 拆分
        List<TermUpdateModel> batches = new ArrayList<>();
        List<TermUpdateModel.Translated> currentBatchTrans = new ArrayList<>();
        int currentChars = updateModel.termsInCsv().length();

        for (TermUpdateModel.Translated trans : updateModel.tableTranslatedList()) {
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


    private List<TermFile.TermEntry> callAIForBatch(TermUpdateModel batch,
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

    static List<TermFile.TermEntry> parseMarkdownTable(String markdown) {
        List<TermFile.TermEntry> entries = new ArrayList<>();
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
                            entries.add(new TermFile.TermEntry(normalized, translated, category, confidence, note));
                        }
                    }
                }

            }
        }
        return entries;
    }

}