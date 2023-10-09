package configgen.data;

import de.siegmar.fastcsv.reader.CsvRow;
import org.dhatim.fastexcel.reader.Row;

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
                        int col) {

        public boolean isCellEmpty() {
            return value.isEmpty();
        }

        public DCell createSub(String sub) {
            return new DCell(sub, rowId, col);
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
    }

    public sealed interface DRawRow {
        String cell(int c);

        int count();
    }

    public record DRawCsvRow(CsvRow row) implements DRawRow {
        @Override
        public String cell(int c) {
            return c < row.getFieldCount() ? row.getField(c).trim() : "";
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
        public int count() {
            return row.getCellCount();
        }
    }


    public void print() {
        stat.print();
        System.out.println(STR. "table count: \t\{ tables.size() }" );
        for (DTable table : tables.values()) {
            System.out.println(table.tableName);
            for (DRawSheet sheet : table.rawSheets) {
                System.out.println(STR. "\t\{ sheet.id() }" );
            }
        }
    }

}