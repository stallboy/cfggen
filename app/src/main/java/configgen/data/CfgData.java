package configgen.data;

import de.siegmar.fastcsv.reader.CsvRow;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.Row;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record CfgData(Map<String, TableData> tables,
                      DataStat stat) {

    public void addSheet(String tableName, SheetData sheetData) {
        TableData tableData = tables.get(tableName);
        if (tableData != null) {
            tableData.sheets.add(sheetData);
        } else {
            List<SheetData> sheets = new ArrayList<>();
            sheets.add(sheetData);
            TableData newTable = new TableData(tableName, sheets);
            tables.put(tableName, newTable);
        }
    }

    public record TableData(String tableName, List<SheetData> sheets) {

    }

    public sealed interface SheetData {
        String id();
    }

    public record ExcelSheetData(String fileName,
                                 String sheetName,
                                 int index,
                                 List<Row> rows) implements SheetData {
        @Override
        public String id() {
            return STR. "\{ fileName } [\{ sheetName }]" ;
        }
    }

    public record CsvData(String fileName,
                          int index,
                          List<CsvRow> rows) implements SheetData {
        @Override
        public String id() {
            return fileName;
        }
    }


    public static class DataStat {
        int csvCount;
        int excelCount;
        int sheetCount;
        int nullCellCount;
        Map<CellType, Integer> cellTypeCountMap = new HashMap<>();

        public int csvCount() {
            return csvCount;
        }

        public int excelCount() {
            return excelCount;
        }

        public int sheetCount() {
            return sheetCount;
        }

        public int nullCellCount() {
            return nullCellCount;
        }

        public Map<CellType, Integer> cellTypeCountMap() {
            return cellTypeCountMap;
        }


        void merge(DataStat s) {
            nullCellCount += s.nullCellCount;
            csvCount += s.csvCount;
            excelCount += s.excelCount;
            sheetCount += s.sheetCount;

            for (Map.Entry<CellType, Integer> e : s.cellTypeCountMap.entrySet()) {
                CellType t = e.getKey();
                int old = cellTypeCountMap.getOrDefault(t, 0);
                cellTypeCountMap.put(t, old + e.getValue());
            }
        }

        public void print() {
            for (Map.Entry<CellType, Integer> entry : cellTypeCountMap.entrySet()) {
                System.out.println(STR. "\{ entry.getKey().toString() }  \{ entry.getValue() }" );
            }
            System.out.println(STR. "null  \{ nullCellCount }" );
            System.out.println(STR. "csv   \{ csvCount }" );
            System.out.println(STR. "excel \{ excelCount }" );
            System.out.println(STR. "sheet \{ sheetCount }" );
        }
    }

}
