package configgen.write;

import configgen.util.Logger;
import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;

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
                this.workbook = WorkbookFactory.create(filePath.toFile(), null, true);
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
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            workbook.write(fos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save Excel file: " + filePath, e);
        } finally {
            // 确保Workbook资源被释放
            try {
                workbook.close();
            } catch (IOException e) {
                Logger.log("Failed to close workbook: " + e.getMessage());
            }
        }
    }


    protected String getCellValue(int row, int col){
        // 行模式：正常读取
        Row sheetRow = sheet.getRow(row);
        if (sheetRow == null) {
            return "";
        }

        Cell cell = sheetRow.getCell(col);
        if (cell == null) {
            return "";
        }

        return formatter.formatCellValue(cell, evaluator);
    }


}