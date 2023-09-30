package configgen.data;

import org.dhatim.fastexcel.reader.CellType;

import java.util.HashMap;
import java.util.Map;

public class DataStat {
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
