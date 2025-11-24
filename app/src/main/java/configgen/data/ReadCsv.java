package configgen.data;

import configgen.util.CSVUtil;
import de.siegmar.fastcsv.reader.CsvRow;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static configgen.data.CfgData.*;
import static configgen.data.ExcelReader.*;

public class ReadCsv {
    private final String defaultEncoding;

    public ReadCsv(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }


    public AllResult readCsv(Path path, Path relativePath,
                             String tableName, int index,
                             char fieldSeparator,
                             String nullableAddTag) {
        CfgDataStat stat = new CfgDataStat();
        List<DRawRow> rows = new ArrayList<>();
        List<CsvRow> read = CSVUtil.read(path, defaultEncoding, fieldSeparator);
        for (CsvRow csvRow : read) {
            rows.add(new DRawCsvRow(csvRow));
            stat.cellCsvCount += csvRow.getFieldCount();
        }

        DRawSheet sheet = new DRawSheet(relativePath.toString(), "", index, rows, new ArrayList<>());
        return new AllResult(List.of(new OneSheetResult(tableName, sheet)), stat, nullableAddTag);
    }

    record DRawCsvRow(CsvRow row) implements DRawRow {
        @Override
        public String cell(int c) {
            return c < row.getFieldCount() ? row.getField(c).trim() : "";
        }

        @Override
        public boolean isCellNumber(int c) {
            return false;
        }

        @Override
        public int count() {
            return row.getFieldCount();
        }
    }

}
