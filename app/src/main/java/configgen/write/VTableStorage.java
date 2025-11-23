package configgen.write;

import configgen.ctx.Context;
import configgen.data.CfgData;
import configgen.data.CfgData.DRowId;
import configgen.data.CfgData.DTable;
import configgen.schema.TableSchema;
import configgen.value.CfgValue;
import configgen.value.CfgValue.VStruct;
import configgen.value.CfgValue.VTable;
import configgen.value.CfgValue.Value;
import configgen.write.RecordBlock.RecordBlockTransformed;
import org.jetbrains.annotations.NotNull;

public class VTableStorage {

    public static void addOrUpdateRecord(@NotNull Context context,
                                         @NotNull VTable vTable,
                                         @NotNull DTable dTable,
                                         @NotNull Value pkValue,
                                         @NotNull VStruct newRecord) {
        RecordBlockMapper mapper = RecordBlockMapper.of(newRecord);
        mapper.map();
        RecordBlock block = mapper.block();

        CfgValue.VStruct oldRecord = vTable.primaryKeyMap().get(pkValue);

        // 1. 文件定位
        TableFile tableFile;
        int startRow;
        int rowCount;
        CfgData.DRawSheet sheet;
        if (oldRecord != null) {
            // 更新操作：从oldRecord获取文件位置，然后删除旧记录
            RecordLoc loc = deleteRecordNoSave(context, oldRecord);
            tableFile = loc.tableFile();
            startRow = loc.startRow;
            rowCount = loc.rowCount;
            sheet = dTable.getByRowId(loc.rowId);

        } else {
            // 新增操作：从dTable获取文件位置
            sheet = TableFileLocator.getSheetFromDTable(dTable);
            boolean isColumnMode = vTable.schema().isColumnMode();
            tableFile = TableFileLocator.createTableFile(sheet.fileName(), sheet.sheetName(), context, isColumnMode);
            startRow = -1; // 放到最后
            rowCount = 0; // 不预留空行
        }

        tableFile.insertRecordBlock(startRow, rowCount, new RecordBlockTransformed(block, sheet.fieldIndices()));
        tableFile.saveAndClose();
    }


    public static void deleteRecord(@NotNull Context context,
                                    @NotNull VStruct oldRecord) {

        RecordLoc r = deleteRecordNoSave(context, oldRecord);
        r.tableFile.saveAndClose();
    }

    private static RecordLoc deleteRecordNoSave(@NotNull Context context,
                                                @NotNull VStruct oldRecord) {

        DRowId rowId = TableFileLocator.getLocFromRecord(oldRecord);
        boolean isColumnMode = ((TableSchema) oldRecord.schema()).isColumnMode();
        TableFile tableFile = TableFileLocator.createTableFile(rowId.fileName(), rowId.sheetName(), context, isColumnMode);

        int startRow = rowId.row();
        int rowCount = tableFile.findRecordRowCount(startRow);
        tableFile.emptyRows(startRow, rowCount);

        return new RecordLoc(tableFile, rowId, startRow, rowCount);
    }


    private record RecordLoc(TableFile tableFile,
                             DRowId rowId,
                             int startRow,
                             int rowCount) {
    }
}
