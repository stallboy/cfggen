package configgen.write;

import configgen.util.Logger;
import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 基于Apache POI的Excel表格文件实现
 */
public abstract class AbstractExcelTableFile implements TableFile {
    protected final Path filePath;
    protected final Workbook workbook;
    protected final Sheet sheet;
    protected final DataFormatter formatter;
    protected final FormulaEvaluator evaluator;
    protected final int headRow;

    /**
     * 构造函数，打开指定的Excel文件和工作表
     *
     * @param filePath Excel文件路径，不能为null
     * @param sheetName 工作表名称，不能为null或空字符串
     * @param headRow 头行号
     */
    public AbstractExcelTableFile(@NotNull Path filePath,
                                  @NotNull String sheetName,
                                  int headRow) {
        this.filePath = filePath;
        this.headRow = headRow;

        if (java.nio.file.Files.exists(filePath)) {
            try {
                this.workbook = WorkbookFactory.create(filePath.toFile(), null, false);
            } catch (IOException e) {
                throw new RuntimeException("Failed to open Excel file: " + filePath, e);
            }
        } else {
            throw new IllegalArgumentException("Excel file does not exist: " + filePath);
        }

        // 获取或创建工作表
        this.sheet = workbook.getSheet(sheetName);
        if (this.sheet == null) {
            try {
                workbook.close();
            } catch (IOException e) {
                Logger.log("Failed to close workbook: " + e.getMessage());
            }
            throw new IllegalArgumentException("Sheet does not exist: " + sheetName + " in " + filePath);
        }

        this.formatter = new DataFormatter();
        this.evaluator = workbook.getCreationHelper().createFormulaEvaluator();
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
        // 1. 确定临时文件路径
        File originalFile = filePath.toFile();
        File tempFile = new File(filePath + ".tmp");

        try {
            // 2. 显式写入临时文件 (这是真正保存数据的时刻)
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                workbook.write(fos);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save to temp file", e);
        } finally {
            // 3. 关闭 Workbook
            // 目的：主要是为了释放对 originalFile 的文件锁
            // 技巧：为了防止 close() 再次尝试写回原文件导致冲突，
            try {
                workbook.close();
            } catch (IOException e) {
                Logger.log("Failed to close workbook: " + e.getMessage());
            }
        }

        // 4. 【关键缺失步骤】用临时文件覆盖源文件
        if (tempFile.exists()) {
            // 如果源文件存在，先删除（或者使用原子移动）
            if (originalFile.exists()) {
                if (!originalFile.delete()) {
                    throw new RuntimeException("无法删除原文件，可能被占用: " + originalFile);
                }
            }
            // 重命名临时文件 -> 原文件
            if (!tempFile.renameTo(originalFile)) {
                throw new RuntimeException("无法重命名临时文件");
            }
        }
    }


    protected String getCellValue(int row, int col) {
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