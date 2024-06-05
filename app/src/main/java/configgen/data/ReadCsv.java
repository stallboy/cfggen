package configgen.data;

import configgen.util.Logger;
import configgen.util.UnicodeReader;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static configgen.data.CfgData.*;
import static configgen.data.CfgDataReader.*;
import static configgen.data.ExcelReader.*;

public class ReadCsv {

    private final String defaultEncoding;

    public ReadCsv(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

    public AllResult readCsv(Path path, Path relativePath, String tableName, int index) throws IOException {
        int count = 0;
        DataStat stat = new DataStat();
        List<DRawRow> rows = new ArrayList<>();
        try (CsvReader reader = CsvReader.builder().build(new UnicodeReader(Files.newInputStream(path), defaultEncoding))) {
            for (CsvRow csvRow : reader) {
                stat.cellCsvCount += csvRow.getFieldCount();
                if (count == 0) {
                    count = csvRow.getFieldCount();
                } else if (count != csvRow.getFieldCount()) {
                    Logger.verbose2("%s %d field count %d not eq %d",
                            path, csvRow.getOriginalLineNumber(), csvRow.getFieldCount(), count);
                }
                rows.add(new DRawCsvRow(csvRow));
            }
        }
        DRawSheet sheet = new DRawSheet(relativePath.toString(), "", index, rows, new ArrayList<>());
        return new AllResult(List.of(new OneSheetResult(tableName, sheet)), stat);
    }
}
