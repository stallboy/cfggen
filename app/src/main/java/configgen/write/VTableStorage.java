package configgen.write;

import configgen.ctx.Context;
import configgen.data.CfgData.DCell;
import configgen.data.CfgData.DRawSheet;
import configgen.data.CfgData.DRowId;
import configgen.data.CfgData.DTable;
import configgen.data.Source;
import configgen.schema.TableSchema;
import configgen.value.CfgValue;
import configgen.value.CfgValue.VInterface;
import configgen.value.CfgValue.VList;
import configgen.value.CfgValue.VMap;
import configgen.value.CfgValue.VStruct;
import configgen.value.CfgValue.VTable;
import configgen.value.CfgValue.Value;
import configgen.write.RecordBlock.RecordBlockTransformed;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 对csv/excel类别的table做add、update、delete
 * 不做任何内存数据结构的修改，只读。
 */
public class VTableStorage {

    public static DRawSheet addOrUpdateRecord(@NotNull Context context,
                                              @NotNull VTable vTable,
                                              @NotNull DTable dTable,
                                              @NotNull Value pkValue,
                                              @NotNull VStruct newRecord) {
        RecordBlock block = RecordBlockMapper.mapToBlock(newRecord);

        CfgValue.VStruct oldRecord = vTable.primaryKeyMap().get(pkValue);

        TableFile tableFile;
        int startRow;
        int rowCount;
        DRawSheet sheet;
        if (oldRecord != null) {
            // 更新操作：从oldRecord获取文件位置，然后删除旧记录
            RecordLoc loc = findRecordLoc(context, oldRecord);
            tableFile = loc.tableFile();
            startRow = loc.startRow;
            rowCount = loc.rowCount;
            sheet = dTable.getSheetByRowId(loc.rowId);
            tableFile.emptyRows(startRow, rowCount, sheet.fieldIndices());

        } else {
            // 新增操作：从dTable获取文件位置
            sheet = TableFileLocator.getSheetFromDTable(dTable);
            boolean isColumnMode = vTable.schema().isColumnMode();
            tableFile = TableFileLocator.createTableFile(sheet.relativeFilePath(), sheet.sheetName(), context, isColumnMode);
            startRow = -1; // 放到最后
            rowCount = 0; // 不预留空行
        }

        tableFile.insertRecordBlock(startRow, rowCount, new RecordBlockTransformed(block, sheet.fieldIndices()));
        tableFile.saveAndClose();

        return sheet;
    }


    public static DRawSheet deleteRecord(@NotNull Context context,
                                         @NotNull DTable dTable,
                                         @NotNull VStruct oldRecord) {

        RecordLoc loc = findRecordLoc(context, oldRecord);
        loc.tableFile.emptyRows(loc.startRow, loc.rowCount, null);
        loc.tableFile.saveAndClose();
        return dTable.getSheetByRowId(loc.rowId);
    }


    private static RecordLoc findRecordLoc(@NotNull Context context,
                                           @NotNull VStruct oldRecord) {

        DRowId rowId = TableFileLocator.getLocFromRecord(oldRecord);
        int startRow = rowId.row();
        int rowCount = computePhysicalRowCount(oldRecord);

        boolean isColumnMode = ((TableSchema) oldRecord.schema()).isColumnMode();
        TableFile tableFile = TableFileLocator.createTableFile(rowId.fileName(), rowId.sheetName(), context, isColumnMode);
        return new RecordLoc(tableFile, rowId, startRow, rowCount);
    }

    /**
     * 计算 oldRecord 在文件里实际占的物理行跨度（max rowId.row - min rowId.row + 1），
     * 作为 emptyRows 的清空行数。
     *
     * <p>不能用 mapToBlock(oldRecord).getRowCount()：那是“逻辑元素行数”，不含文件里的人工空
     * 分隔行；策划若在表里加了视觉空行，mapToBlock 算出的行数会比文件实际小，emptyRows 清不干净，
     * 残留的旧行在下次 reload 时会被当成新元素，导致“每次更新多出节点”。
     *
     * <p>这里递归整棵 value 树收集所有 DCell 的物理行号。CellParser 会把全空行从内存表过滤掉，
     * 但保留下来数据行的物理行号会跳号，于是 max-min+1 自然覆盖被跳过的空行。顶层 record 的
     * source 只含首行 cell，嵌套 block 后续行的 cell 散落在子 Value 的 source 里，所以必须递归。
     */
    private static int computePhysicalRowCount(VStruct record) {
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        boolean found = false;

        Deque<Value> stack = new ArrayDeque<>();
        stack.push(record);
        while (!stack.isEmpty()) {
            Value v = stack.pop();
            switch (v.source()) {
                case DCell c -> {
                    int r = c.rowId().row();
                    if (r < minRow) minRow = r;
                    if (r > maxRow) maxRow = r;
                    found = true;
                }
                case Source.DCellList cl -> {
                    for (DCell c : cl.cells()) {
                        int r = c.rowId().row();
                        if (r < minRow) minRow = r;
                        if (r > maxRow) maxRow = r;
                    }
                    found = true;
                }
                case Source.DFile ignored -> { } // JSON 表不走 VTableStorage，理论不会到这里
            }
            switch (v) {
                case VStruct vs -> {
                    for (Value child : vs.values()) {
                        stack.push(child);
                    }
                }
                case VInterface vi -> {
                    for (Value child : vi.child().values()) {
                        stack.push(child);
                    }
                }
                case VList vl -> {
                    for (var sv : vl.valueList()) {
                        stack.push(sv);
                    }
                }
                case VMap vm -> {
                    for (var entry : vm.valueMap().entrySet()) {
                        stack.push(entry.getKey());
                        stack.push(entry.getValue());
                    }
                }
                default -> { } // primitive 等无子节点
            }
        }
        return found ? (maxRow - minRow + 1) : 1;
    }


    private record RecordLoc(TableFile tableFile,
                             DRowId rowId,
                             int startRow,
                             int rowCount) {
    }
}
