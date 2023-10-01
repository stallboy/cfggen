package configgen.data;

import configgen.schema.Stat;

public class DataStat implements Stat {
    int tableCount;
    int csvCount;
    int excelCount;
    int sheetCount;
    int rowCount;
    int columnCount;

    int emptyTableCount;
    int ignoredSheetCount;
    int ignoredCsvCount;
    int ignoredColumnCount;
    int ignoredRowCount;

    int cellCsvCount;
    int cellNumberCount;
    int cellStrCount;
    int cellBoolCount;
    int cellEmptyCount;
    int cellNullCount;
    int cellFormulaCount;
    int cellErrCount;

    public int tableCount() {
        return tableCount;
    }

    public int csvCount() {
        return csvCount;
    }

    public int excelCount() {
        return excelCount;
    }

    public int sheetCount() {
        return sheetCount;
    }

    public int rowCount() {
        return rowCount;
    }

    public int columnCount() {
        return columnCount;
    }

    public int emptyTableCount() {
        return emptyTableCount;
    }

    public int ignoredSheetCount() {
        return ignoredSheetCount;
    }

    public int ignoredCsvCount() {
        return ignoredCsvCount;
    }

    public int ignoredColumnCount() {
        return ignoredColumnCount;
    }

    public int ignoredRowCount() {
        return ignoredRowCount;
    }

    public int cellCsvCount() {
        return cellCsvCount;
    }

    public int cellNumberCount() {
        return cellNumberCount;
    }

    public int cellStrCount() {
        return cellStrCount;
    }

    public int cellBoolCount() {
        return cellBoolCount;
    }

    public int cellEmptyCount() {
        return cellEmptyCount;
    }

    public int cellNullCount() {
        return cellNullCount;
    }

    public int cellFormulaCount() {
        return cellFormulaCount;
    }

    public int cellErrCount() {
        return cellErrCount;
    }

    public static void main(String[] args) {
        new DataStat().print();
    }
}
