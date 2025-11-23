package configgen.write;

import configgen.util.Logger;
import de.siegmar.fastcsv.writer.CsvWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;


public class CsvTableFile implements TableFile {
    private final Path filePath;
    private final char fieldSeparator;
    private final List<List<String>> rows;
    private boolean modified = false;

    public CsvTableFile(Path filePath, char fieldSeparator) throws IOException {
        this.filePath = filePath;
        this.fieldSeparator = fieldSeparator;
        this.rows = new ArrayList<>();

        // 读取现有文件或创建新文件
        if (Files.exists(filePath)) {
            loadExistingFile();
        }
    }

    private void loadExistingFile() throws IOException {
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);

        // 使用简单的CSV解析
        for (String line : lines) {
            List<String> fields = parseCsvLine(line, fieldSeparator);
            rows.add(new ArrayList<>(fields));
        }
    }

    /**
     * 简单的CSV行解析
     */
    private List<String> parseCsvLine(String line, char separator) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == separator && !inQuotes) {
                fields.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        // 添加最后一个字段
        fields.add(currentField.toString().trim());
        return fields;
    }

    @Override
    public void emptyRows(int startLine, int count) {
        if (startLine < 0 || count <= 0 || startLine >= rows.size()) {
            return;
        }

        int endLine = Math.min(startLine + count, rows.size());

        // 清空指定范围内的行
        for (int i = startLine; i < endLine; i++) {
            List<String> row = rows.get(i);
            for (int j = 0; j < row.size(); j++) {
                row.set(j, "");
            }
        }
        modified = true;
    }

    @Override
    public void insertRecordBlock(int startLine, int emptyRowCount, RecordBlock content) {
        if (content == null || content.getRowCount() <= 0) {
            return;
        }

        int actualStartLine;
        if (startLine == -1) {
            // 放到最后
            actualStartLine = rows.size();
        } else {
            actualStartLine = startLine;
        }

        int contentLineCount = content.getRowCount();

        // 如果内容行数大于可用空行数，需要插入新行
        if (contentLineCount > emptyRowCount && startLine != -1) {
            int insertCount = contentLineCount - emptyRowCount;
            for (int i = 0; i < insertCount; i++) {
                rows.add(actualStartLine + emptyRowCount + i, createEmptyRow());
            }
        }

        // 确保有足够的行
        while (rows.size() < actualStartLine + contentLineCount) {
            rows.add(createEmptyRow());
        }

        // 写入记录块内容
        for (int rowOffset = 0; rowOffset < contentLineCount; rowOffset++) {
            int rowNum = actualStartLine + rowOffset;
            List<String> row = rows.get(rowNum);

            // 确保行有足够的列
            int maxCols = getMaxColumnCount();
            while (row.size() < maxCols) {
                row.add("");
            }

            // 具体的单元格写入逻辑在RecordBlock实现中处理
            // RecordBlock会通过cell方法设置具体的值
        }
        modified = true;
    }

    @Override
    public void save() {
        if (!modified) {
            return;
        }

        try (CsvWriter writer = CsvWriter.builder()
                .fieldSeparator(fieldSeparator)
                .build(filePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            for (List<String> row : rows) {
                writer.writeRow(row);
            }

        } catch (IOException e) {
            Logger.log("Failed to save CSV file: " + filePath + " - " + e.getMessage());
            throw new RuntimeException("Failed to save CSV file: " + filePath, e);
        }

        modified = false;
    }

    @Override
    public String getCell(int row, int col) {
        return "";
    }

    /**
     * 获取最大列数
     */
    private int getMaxColumnCount() {
        int maxCols = 0;
        for (List<String> row : rows) {
            maxCols = Math.max(maxCols, row.size());
        }
        return maxCols;
    }

    /**
     * 创建空行
     */
    private List<String> createEmptyRow() {
        int maxCols = getMaxColumnCount();
        List<String> row = new ArrayList<>(maxCols);
        for (int i = 0; i < maxCols; i++) {
            row.add("");
        }
        return row;
    }

    /**
     * 设置单元格的值
     */
    public void setCellValue(int row, int col, String value) {
        // 确保行存在
        while (rows.size() <= row) {
            rows.add(createEmptyRow());
        }

        List<String> rowData = rows.get(row);

        // 确保列存在
        while (rowData.size() <= col) {
            rowData.add("");
        }

        rowData.set(col, value != null ? value : "");
        modified = true;
    }

    /**
     * 获取单元格的值
     */
    public String getCellValue(int row, int col) {
        if (row < 0 || row >= rows.size()) {
            return "";
        }

        List<String> rowData = rows.get(row);
        if (col < 0 || col >= rowData.size()) {
            return "";
        }

        return rowData.get(col);
    }
}