package configgen.data;

import configgen.util.LocaleUtil;
import configgen.util.Logger;

import java.util.List;
import java.util.Map;


/**
 * 数据模型
 */
public record CfgData(Map<String, DTable> tables,
                      DataStat stat) {

    /**
     * @param tableName 程序员的表名称
     * @param fields    head信息
     * @param rows      无head，去空行，去注释行，去注释列。规整的相同列数的row，列数不为0
     * @param rawSheets 原始的excel[sheet]信息
     *                  约定表格必须在同一个文件夹下，且名称为 xxx 或者 xxx_0,xxx_1,xxx_2,xxx_3 ...
     *                  比如task表，如果是csv配置，可以拆分成：task.csv(同task_0.csv)，task_1.csv，task_2.csv
     *                  如果是excel配置，excel中的页签名称可以拆分成：task(同task_0)，task_1，task_2
     */
    public record DTable(String tableName,
                         List<DField> fields,   // by HeadParser
                         List<List<DCell>> rows,    // by CellParser
                         List<DRawSheet> rawSheets) {   // by CfgDataReader
    }

    public record DField(String name,
                         String comment) {
    }

    /**
     * @param value 已trim过的value
     * @param rowId rowId.row，col 是逻辑上的行号和列号，要得到excel文件中具体的行和列需要外界提供isColumnMode
     * @param col   列号
     */
    public record DCell(String value,
                        DRowId rowId,
                        int col,
                        byte mode) {

        public static final DCell EMPTY = of("");

        public static DCell of(String str) {
            return new DCell(str, new CfgData.DRowId("fileName", "sheetName", 0), 0, (byte) 0);
        }

        public static final byte COLUMN_MODE = 0x1;
        public static final byte CELL_NUMBER = 0x2;
        public static final byte CELL_NUMBER_WITH_COMMA = 0x4;

        public static byte modeOf(boolean isColumnMode, boolean isCellNumber, boolean isCellNumberWithComma) {
            byte res = 0;
            if (isColumnMode) {
                res |= COLUMN_MODE;
            }
            if (isCellNumber) {
                res |= CELL_NUMBER;
            }
            if (isCellNumberWithComma) {
                res |= CELL_NUMBER_WITH_COMMA;
            }
            return res;
        }

        public boolean isColumnMode() {
            return (mode & COLUMN_MODE) != 0;
        }

        public boolean isCellNumberWithComma() {
            return (mode & CELL_NUMBER_WITH_COMMA) != 0;
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
            if (isColumnMode()) {
                r = col;
                c = rowId.row;
            } else {
                r = rowId.row;
                c = col;
            }
            return LocaleUtil.getMessage("CellToString", sheet, r + 1, toAZ(c), value);
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

    public void print() {
        stat.print();
        Logger.verbose2("table count: %d", tables.size());
        for (DTable table : tables.values()) {
            Logger.verbose2(table.tableName);
            for (DRawSheet sheet : table.rawSheets) {
                Logger.verbose2("\t%s", sheet.id());
            }
        }
    }

}
