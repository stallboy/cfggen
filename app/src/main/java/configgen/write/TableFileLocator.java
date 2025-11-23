package configgen.write;

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

    /**
     * 从oldRecord中获取文件位置信息
     */
    public static DRowId getLocFromRecord(@NotNull VStruct record) {
        DRowId rowId = null;
        switch (record.source()) {
            case CfgData.DCell dCell -> {
                rowId = dCell.rowId();
            }
            case Source.DCellList dCellList -> {
                if (!dCellList.cells().isEmpty()) {
                    rowId = dCellList.cells().getFirst().rowId();
                }
            }
            case Source.DFile ignored -> {
            }
        }
        return rowId;
    }

    /**
     * 从dTable中获取文件位置信息
     */
    public static DRowId getLocFromDTable(@NotNull CfgData.DTable dTable) {
        if (dTable.rawSheets().isEmpty()) {
            return null;
        }

        // 使用最后一个rawSheet
        CfgData.DRawSheet lastSheet = dTable.rawSheets().getLast();
        String fileName = lastSheet.fileName();
        String sheetName = lastSheet.sheetName();

        // 对于新增记录，行号设为-1表示放到最后
        return new DRowId(fileName, sheetName, -1);
    }

    /**
     * 创建TableFile实例
     */
    public static TableFile createTableFile(@NotNull DRowId location,
                                            @NotNull Path dataDir,
                                            boolean isColumnMode,
                                            int headRow) {
        Path filePath = dataDir.resolve(location.fileName());

        // 根据文件扩展名判断文件类型
        String fileName = location.fileName().toLowerCase();
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            try {
                if (isColumnMode) {
                    return new ColumnModeExcelTableFile(filePath, location.sheetName(), headRow);
                } else {
                    return new ExcelTableFile(filePath, location.sheetName(), headRow);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create ExcelTableFile: " + filePath, e);
            }
        } else if (fileName.endsWith(".csv")) {
            try {
                // 默认使用逗号作为分隔符，传递headRow参数
                return new CsvTableFile(filePath, ',', headRow);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create CsvTableFile: " + filePath, e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + fileName);
        }
    }


}