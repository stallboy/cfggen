package configgen.data;

import configgen.util.Localize;
import configgen.util.Logger;
import de.siegmar.fastcsv.reader.CsvRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.dhatim.fastexcel.reader.Row;

import java.util.List;
import java.util.Map;

import static org.apache.poi.ss.usermodel.CellType.NUMERIC;


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

        public static final byte COLUMN_MODE = 0x1;
        public static final byte CELL_NUMBER_WITH_COMMA = 0x2;

        public static byte modeOf(boolean isColumnMode, boolean isCellNumberWithComma) {
            byte res = 0;
            if (isColumnMode) {
                res |= COLUMN_MODE;
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
            return Localize.getMessage("CellToString", sheet, r + 1, toAZ(c), value);
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
                            List<Integer> fieldIndices) {
        public String id() {
            if (sheetName.isEmpty()) {
                return fileName;
            }
            return STR. "\{ fileName }[\{ sheetName }]" ;
        }

        public boolean isCsv() {
            return sheetName.isEmpty();
        }
    }

    public sealed interface DRawRow {
        String cell(int c);

        boolean isCellNumberWithComma(int c);

        int count();
    }

    public record DRawCsvRow(CsvRow row) implements DRawRow {
        @Override
        public String cell(int c) {
            return c < row.getFieldCount() ? row.getField(c).trim() : "";
        }

        @Override
        public boolean isCellNumberWithComma(int c) {
            return false;
        }

        @Override
        public int count() {
            return row.getFieldCount();
        }
    }


    public record DRawExcelRow(Row row) implements DRawRow {
        @Override
        public String cell(int c) {
            return row.getCellText(c).trim();
        }

        @Override
        public boolean isCellNumberWithComma(int c) {
            return false; // fastexcel 没有读style信息，所以没法查找是否有显示的逗号
        }

        @Override
        public int count() {
            return row.getCellCount();
        }
    }

    public record DRawPoiExcelRow(org.apache.poi.ss.usermodel.Row row,
                                  DRawPoiFmt fmt) implements DRawRow {
        @Override
        public String cell(int c) {
            Cell cell = row.getCell(c);
            if (cell != null) {
                return fmt.formatter.formatCellValue(cell, fmt.evaluator).trim();
            } else {
                return "";
            }
        }

        @Override
        public boolean isCellNumberWithComma(int c) {
            Cell cell = row.getCell(c);
            return cell != null && cell.getCellType() == NUMERIC; //这里只判断数字，外部判断comma
        }

        @Override
        public int count() {
            return row.getLastCellNum();
        }
    }

    public record DRawPoiFmt(DataFormatter formatter,
                             FormulaEvaluator evaluator) {
    }


    public void print() {
        stat.print();
        Logger.verbose2(STR. "table count: \t\{ tables.size() }" );
        for (DTable table : tables.values()) {
            Logger.verbose2(table.tableName);
            for (DRawSheet sheet : table.rawSheets) {
                Logger.verbose2(STR. "\t\{ sheet.id() }" );
            }
        }
    }

}