package configgen.data;

import de.siegmar.fastcsv.reader.CsvRow;
import org.dhatim.fastexcel.reader.Row;

import java.util.List;
import java.util.Map;


public record CfgData(Map<String, DTable> tables,
                      DataStat stat) {

    public record DTable(String tableName,
                         List<DField> fields,   // by HeadParser
                         List<DCell[]> rows,    // by CellParser
                         List<DRawSheet> rawSheets) {   // by CfgDataReader
    }

    public record DField(String name,
                         String comment) {
    }

    public record DCell(String value,
                        DRowId rowId,
                        int col) {
    }

    public record DRowId(String fileName,
                         String sheetName,
                         int row) {
    }


    public record DRawSheet(String fileName,
                            String sheetName,  // empty when file is csv
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