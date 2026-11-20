package configgen.write;

import org.jspecify.annotations.Nullable;

import configgen.util.FileUtil;
import configgen.util.Logger;
import configgen.write.RecordBlock.RecordBlockTransformed;
import org.apache.poi.ss.usermodel.*;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 基于Apache POI的Excel表格文件实现
 * <p>
 * 行模式与列模式的 {@link #emptyRows} / {@link #insertRecordBlock} 共用同一骨架，
 * 方向相关的操作由子类通过钩子方法提供（行模式下"线"=行，列模式下"线"=列）。
 */
public abstract class AbstractExcelTableFile implements TableFile {
    protected final Path filePath;
    protected final Workbook workbook;
    protected final Sheet sheet;
    protected final DataFormatter formatter;
    protected final FormulaEvaluator evaluator;
    protected final int headRow;
    private boolean closed;

    /**
     * 构造函数，打开指定的Excel文件和工作表
     *
     * @param filePath Excel文件路径，不能为null
     * @param sheetName 工作表名称，不能为null或空字符串
     * @param headRow 头行号
     */
    public AbstractExcelTableFile(Path filePath,
                                  String sheetName,
                                  int headRow) {
        this.filePath = filePath;
        this.headRow = headRow;

        if (java.nio.file.Files.exists(filePath)) {
            try (InputStream input = Files.newInputStream(filePath)) {
                this.workbook = WorkbookFactory.create(input);
            } catch (IOException e) {
                throw new RuntimeException("Failed to open Excel file: " + filePath, e);
            }
        } else {
            throw new IllegalArgumentException("Excel file does not exist: " + filePath);
        }

        try {
            this.sheet = workbook.getSheet(sheetName);
            if (this.sheet == null) {
                throw new IllegalArgumentException("Sheet does not exist: " + sheetName + " in " + filePath);
            }
            this.formatter = new DataFormatter();
            this.evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        } catch (RuntimeException | Error failure) {
            try {
                workbook.close();
            } catch (IOException | RuntimeException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw failure;
        }
    }


    /**
     * 保存文件并关闭所有资源
     *
     * <p>该方法会将所有修改写入文件，并释放Workbook资源。
     * 调用此方法后，对象将不再可用。
     *
     * @throws RuntimeException 如果保存文件失败
     */
    @Override
    public void saveAndClose() {
        try {
            FileUtil.writeReplacement(filePath, staged -> {
                try (var output = Files.newOutputStream(staged)) {
                    workbook.write(output);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to save Excel file: " + filePath, e);
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            try {
                workbook.close();
            } catch (IOException e) {
                Logger.log("Failed to close workbook: " + e.getMessage());
            }
        }
    }


    /**
     * 清空指定范围的数据（行模式下清空行，列模式下清空列）
     *
     * @param startLine 起始行/列号（从0开始）
     * @param count    要清空的行/列数
     * @param fieldIndices 如果为null表示第一行/列全部清空，如果不为null表示第一行/列只清空指定indices下的数据
     */
    @Override
    public void emptyRows(int startLine, int count, @Nullable List<Integer> fieldIndices) {
        if (startLine < 0 || count <= 0) {
            return;
        }

        int lastLineNum = lastLineNum();
        if (startLine > lastLineNum) {
            return;
        }

        // 清空指定范围内的行/列
        for (int i = startLine; i < Math.min(startLine + count, lastLineNum + 1); i++) {
            if (i == startLine && fieldIndices != null) {
                // 只清空指定 indices下的数据
                for (int cellIndex : fieldIndices) {
                    blankCell(i, cellIndex);
                }
            } else {
                // 清空行/列中的所有单元格
                blankLine(i);
            }
        }
    }

    /**
     * 插入记录块到指定位置
     *
     * @param startLine 起始行/列号，-1表示放到最后
     * @param emptyLineCount 可用的空行/列数
     * @param content 记录块内容，包含要插入的数据
     */
    @Override
    public void insertRecordBlock(int startLine, int emptyLineCount, RecordBlockTransformed content) {
        int contentLineCount = content.getRowCount();
        if (contentLineCount <= 0) {
            return;
        }

        int actualStartLine;
        if (startLine == -1) {
            // 放到最后
            actualStartLine = Math.max(appendLineNum(), headRow);
        } else {
            actualStartLine = startLine;
        }

        // 如果内容行/列数大于可用空行/列数，需要移动后续行/列
        if (contentLineCount > emptyLineCount && startLine != -1) {
            shiftLines(actualStartLine + emptyLineCount, contentLineCount - emptyLineCount);
        }

        // 写入记录块内容
        for (int lineOffset = 0; lineOffset < contentLineCount; lineOffset++) {
            writeLine(actualStartLine + lineOffset, content.getRow(lineOffset));
        }
    }

    /**
     * @return 最后一行/列的index（0-based，无数据时为-1）
     */
    protected abstract int lastLineNum();

    /**
     * 把指定行/列中指定位置的单元格置为blank（行/列或单元格不存在时忽略）
     */
    protected abstract void blankCell(int line, int index);

    /**
     * 把指定行/列中的所有单元格置为blank
     */
    protected abstract void blankLine(int line);

    /**
     * @return 追加模式下应写入的起始行/列号
     */
    protected abstract int appendLineNum();

    /**
     * 将from位置及后续的行/列移动count个位置
     */
    protected abstract void shiftLines(int from, int count);

    /**
     * 把一行/列内容写入lineNum位置，lineData为null时的处理由方向决定
     */
    protected abstract void writeLine(int lineNum, @Nullable String @Nullable [] lineData);


    protected @Nullable String getCellValue(int row, int col) {
        // 行模式：正常读取
        Row sheetRow = sheet.getRow(row);
        if (sheetRow == null) {
            return null;
        }

        Cell cell = sheetRow.getCell(col);
        if (cell == null) {
            return null;
        }

        return formatter.formatCellValue(cell, evaluator);
    }


}