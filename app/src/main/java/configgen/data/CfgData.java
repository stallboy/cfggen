package configgen.data;

import configgen.util.LocaleUtil;
import configgen.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * 数据模型
 */
public record CfgData(Map<String, DTable> tables,
                      CfgDataStat stat) {

    public CfgData {
        Objects.requireNonNull(tables);
        Objects.requireNonNull(stat);
    }

    /**
     * @param tableName 程序员的表名称
     * @param fields    head信息
     * @param rows      无head，去空行，去注释行，去注释列。规整的相同列数的row，列数不为0
     * @param rawSheets 原始的excel[sheet]信息
     *                  约定表格必须在同一个文件夹下，且名称为 xxx 或者 xxx_0,xxx_1,xxx_2,xxx_3 ...
     *                  比如task表，如果是csv配置，可以拆分成：task.csv(同task_0.csv)，task_1.csv，task_2.csv
     *                  如果是excel配置，excel中的页签名称可以拆分成：task(同task_0)，task_1，task_2
     * @param nullableAddTag 可选的附加标记，一般是-client，-server，用于提取特定数据
     */
    public record DTable(String tableName,
                         List<DField> fields,       // by HeadParser
                         List<List<DCell>> rows,    // by CellParser
                         List<DRawSheet> rawSheets, // by CfgDataReader
                         String nullableAddTag) {
        public DTable {
            Objects.requireNonNull(tableName);
            Objects.requireNonNull(fields);
            Objects.requireNonNull(rows);
            Objects.requireNonNull(rawSheets);
        }

        public static DTable of(String tableName, List<DRawSheet> rawSheets) {
            return of(tableName, rawSheets, null);
        }

        public static DTable of(String tableName, List<DRawSheet> rawSheets, String nullableAddTag) {
            return new DTable(tableName, new ArrayList<>(), new ArrayList<>(), rawSheets, nullableAddTag);
        }
    }

    public record DField(String name,
                         String comment,
                         String suggestedType) {

        public DField {
            Objects.requireNonNull(name);
            Objects.requireNonNull(comment);
            Objects.requireNonNull(suggestedType);
        }
    }


    public static final class DCell implements Source {
        private final String value;
        private final DRowId rowId;
        private final int col;
        private byte mode;

        /**
         * @param value 已trim过的value
         * @param rowId rowId.row，col 是逻辑上的行号和列号，要得到excel文件中具体的行和列需要外界提供isColumnMode
         * @param col   列号
         */
        public DCell(String value, DRowId rowId, int col, byte mode) {
            Objects.requireNonNull(value);
            Objects.requireNonNull(rowId);
            this.value = value;
            this.rowId = rowId;
            this.col = col;
            this.mode = mode;
        }


        public static DCell of(String content, String fileName) {
            return new DCell(content, new CfgData.DRowId(fileName, "", 0), 0, CELL_FAKE);
        }

        public String value() {
            return value;
        }

        public DRowId rowId() {
            return rowId;
        }

        public int col() {
            return col;
        }

        public byte mode() {
            return mode;
        }

        public void setModePackOrSep() {
            this.mode |= CELL_PACK_OR_SEP;
        }

        public boolean isModePackOrSep() {
            return (mode & CELL_PACK_OR_SEP) != 0;
        }

        public static final byte COLUMN_MODE = 0x1;
        public static final byte CELL_NUMBER = 0x2;
        public static final byte CELL_FAKE = 0x4;
        public static final byte CELL_PACK_OR_SEP = 0x8;

        public static byte modeOf(boolean isColumnMode, boolean isCellNumber) {
            byte res = 0;
            if (isColumnMode) {
                res |= COLUMN_MODE;
            }
            if (isCellNumber) {
                res |= CELL_NUMBER;
            }
            return res;
        }

        public boolean isCellEmpty() {
            return value.isEmpty();
        }

        public DCell createSub(String sub) {
            return new DCell(sub, rowId, col, mode);
        }

        @Override
        public String toString() {
            String sheet = rowId.sheetName.isEmpty() ? rowId.fileName :
                    String.format("%s[%s]", rowId.fileName, rowId.sheetName);
            int r;
            int c;
            if ((mode & COLUMN_MODE) != 0) {
                r = col;
                c = rowId.row;
            } else {
                r = rowId.row;
                c = col;
            }
            return LocaleUtil.getFormatedLocaleString("CellToString",
                    "sheet={0},row={1},col={2},data={3}",
                    sheet, r + 1, toAZ(c), value);
        }

        private static final int N = 'Z' - 'A' + 1;

        private static String toAZ(int v) {
            int q = v / N;
            String r = String.valueOf((char) ('A' + (v % N)));
            if (q > 0) {
                return toAZ(q - 1) + r;
            } else {
                return r;
            }
        }

    }

    public record DRowId(String fileName,
                         String sheetName,
                         int row) {
    }

    /**
     * @param fileName     文件名，支持csv和excel
     * @param sheetName    当文件时csv时，为空
     * @param index        支持多个csv或sheet组成一个逻辑的table，此时index用于数据排序
     * @param rows         原始每个格子里的数据
     * @param fieldIndices csv或sheet中第二行是程序用名，做为field跟schema对应，可以为空，用于策划注释。
     *                     fieldIndices用于把field给挑选出来。
     */
    public record DRawSheet(String fileName,
                            String sheetName,
                            int index,
                            List<DRawRow> rows,
                            // HeaderParser填写
                            List<Integer> fieldIndices) {
        public DRawSheet {
            Objects.requireNonNull(fileName);
            Objects.requireNonNull(sheetName);
            Objects.requireNonNull(rows);
            Objects.requireNonNull(fieldIndices);
        }

        public String id() {
            if (sheetName.isEmpty()) {
                return fileName;
            }
            return String.format("%s[%s]", fileName, sheetName);
        }

        public boolean isCsv() {
            return sheetName.isEmpty();
        }
    }

    public interface DRawRow {
        String cell(int c);

        boolean isCellNumber(int c);

        int count();
    }

    public void verbosePrintStat() {
        if (Logger.verboseLevel() > 0) {
            stat.print();
        }
        if (Logger.verboseLevel() > 1) {
            Logger.log("table count: %d", tables.size());
            for (DTable table : tables.values()) {
                Logger.log(table.tableName);
                for (DRawSheet sheet : table.rawSheets) {
                    Logger.log("\t%s", sheet.id());
                }
            }
        }

    }

}
