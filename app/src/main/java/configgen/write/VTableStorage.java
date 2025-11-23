package configgen.write;

import configgen.ctx.Context;
import configgen.data.CfgData;
import configgen.data.CfgData.DRowId;
import configgen.data.CfgData.DTable;
import configgen.value.CfgValue;
import configgen.value.CfgValue.VStruct;
import configgen.value.CfgValue.VTable;
import configgen.value.CfgValue.Value;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

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
        RecordLoc recordLoc;
        if (oldRecord != null) {
            // 更新操作：从oldRecord获取文件位置，然后删除旧记录
            recordLoc = deleteRecordNoSave(context, oldRecord);
        } else {
            // 新增操作：从dTable获取文件位置
            DRowId loc = TableFileLocator.getLocFromDTable(dTable);
            Path dataDir = context.getContextCfg().dataDir();
            TableFile tableFile = TableFileLocator.createTableFile(loc, dataDir);
            recordLoc = new RecordLoc(tableFile, -1, -1);
        }
        TableFile tableFile = recordLoc.tableFile();
        tableFile.insertRecordBlock(recordLoc.startRow, recordLoc.rowCount, block);
        tableFile.save();
    }


    public static void deleteRecord(@NotNull Context context,
                                    @NotNull VStruct oldRecord) {

        RecordLoc r = deleteRecordNoSave(context, oldRecord);
        r.tableFile.save();
    }

    private static RecordLoc deleteRecordNoSave(@NotNull Context context,
                                                @NotNull VStruct oldRecord) {

        DRowId location = TableFileLocator.getLocFromRecord(oldRecord);
        Path dataDir = context.getContextCfg().dataDir();
        TableFile tableFile = TableFileLocator.createTableFile(location, dataDir);

        int startRow = location.row();
        int rowCount = tableFile.findRecordRowCount(startRow);
        tableFile.emptyRows(startRow, rowCount);

        return new RecordLoc(tableFile, startRow, rowCount);

    }


    private record RecordLoc(TableFile tableFile,
                             int startRow,
                             int rowCount) {

    }
}
