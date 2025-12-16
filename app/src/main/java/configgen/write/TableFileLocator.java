package configgen.write;

import configgen.ctx.Context;
import configgen.data.CfgData;
import configgen.data.CfgData.DRowId;
import configgen.data.Source;
import configgen.value.CfgValue.VStruct;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * 表格文件定位工具类
 * 用于定位VStruct对应的Excel/CSV文件位置
 */
public class TableFileLocator {

    public static DRowId getLocFromRecord(@NotNull VStruct record) {
        switch (record.source()) {
            case CfgData.DCell dCell -> {
                return dCell.rowId();
            }
            case Source.DCellList dCellList -> {
                if (!dCellList.cells().isEmpty()) {
                    return dCellList.cells().getFirst().rowId();
                } else {
                    throw new IllegalArgumentException("DCellList is empty in record: " + record);
                }
            }
            case Source.DFile ignored -> {
                throw new IllegalArgumentException("Record source is DFile, cannot get row location: " + record);
            }
        }
    }

    /**
     * 从dTable中获取文件位置信息
     */
    public static CfgData.DRawSheet getSheetFromDTable(@NotNull CfgData.DTable dTable) {
        if (dTable.rawSheets().isEmpty()) {
            throw new IllegalArgumentException("DTable has no rawSheets: " + dTable.tableName());
        }

        // 使用最后一个rawSheet
        return dTable.rawSheets().getLast();
    }

    /**
     * 创建TableFile实例
     */
    public static TableFile createTableFile(@NotNull String fileName,
                                            @NotNull String sheetName,
                                            @NotNull Context context,
                                            boolean isColumnMode) {
        Path dataDir = context.contextCfg().dataDir();
        int headRow = context.contextCfg().headRow().rowCount();

        Path filePath = dataDir.resolve(fileName);

        // 根据文件扩展名判断文件类型
        String toLowerFileName = fileName.toLowerCase();
        if (toLowerFileName.endsWith(".xlsx") || toLowerFileName.endsWith(".xls")) {
            try {
                if (isColumnMode) {
                    return new ColumnModeExcelTableFile(filePath, sheetName, headRow);
                } else {
                    return new ExcelTableFile(filePath, sheetName, headRow);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create ExcelTableFile: " + filePath, e);
            }
        } else if (toLowerFileName.endsWith(".csv")) {
            try {
                String encoding = context.contextCfg().csvOrTsvDefaultEncoding();
                if (isColumnMode) {
                    return new ColumnModeCsvTableFile(filePath, encoding, headRow);
                } else {
                    return new CsvTableFile(filePath, encoding, headRow);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create CsvTableFile: " + filePath, e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + fileName);
        }
    }


}