package configgen.write;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CSV表格文件实现（行模式）
 */
public class CsvTableFile extends AbstractCsvTableFile {
    private final int fixedMaxColumnCount;

    public CsvTableFile(Path filePath, String defaultEncoding, int headRow) {
        super(filePath, defaultEncoding, headRow);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV file has no data: " + filePath);
        }
        fixedMaxColumnCount = rows.getFirst().size();
    }

    @Override
    protected int lineCount() {
        return rows.size();
    }

    @Override
    protected void emptyCell(int line, int index) {
        List<String> row = rows.get(line);
        if (index >= 0 && index < row.size()) {
            row.set(index, "");
        }
    }

    @Override
    protected void emptyLine(int line) {
        Collections.fill(rows.get(line), "");
    }

    @Override
    protected void insertGapLines(int from, int count) {
        for (int i = 0; i < count; i++) {
            rows.add(from + i, createEmptyRow());
        }
    }

    @Override
    protected void ensureCapacity(int required) {
        // 确保有足够的行
        while (rows.size() < required) {
            rows.add(createEmptyRow());
        }
    }

    @Override
    protected void writeLine(int lineNum, String[] lineData, int capacity) {
        List<String> row = rows.get(lineNum);
        for (int col = 0; col < lineData.length; col++) {
            String cellValue = lineData[col];
            if (cellValue != null) {
                row.set(col, cellValue);
            }
        }
    }

    private List<String> createEmptyRow() {
        return new ArrayList<>(Collections.nCopies(fixedMaxColumnCount, ""));
    }

}
