package configgen.data;

import configgen.util.CSVUtil;
import de.siegmar.fastcsv.reader.CsvRecord;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static configgen.data.CfgData.*;

public class ReadCsv {
    private final String defaultEncoding;

    public ReadCsv(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }


    public ReadResult readCsv(@NotNull Path path,
                              @NotNull Path relativePath,
                              @NotNull String tableName,
                              int index,
                              char fieldSeparator,
                              String nullableAddTag) {
        CfgDataStat stat = new CfgDataStat();
        List<DRawRow> rows = new ArrayList<>();
        List<CsvRecord> read = CSVUtil.read(path, defaultEncoding, fieldSeparator);
        for (CsvRecord csvRow : read) {
            rows.add(new DRawCsvRow(csvRow));
            stat.cellCsvCount += csvRow.getFieldCount();
        }

        DRawSheet sheet = new DRawSheet(relativePath.toString(), "", index, rows, new ArrayList<>());
        return new ReadResult(List.of(new ReadResult.OneSheet(tableName, sheet)), stat, nullableAddTag);
    }

    record DRawCsvRow(CsvRecord row) implements DRawRow {
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
