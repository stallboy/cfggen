package configgen.geni18n;

import configgen.geni18n.TodoFile.Line;
import configgen.util.CSVUtil;
import de.siegmar.fastcsv.reader.CsvRow;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TermCfg(@NotNull List<TermCfgItem> items) {

    public record TermCfgItem(@NotNull String table,
                              @NotNull String fieldChain) {
    }

    public static TermCfg load(Path termCfgFile) {
        List<CsvRow> rows = CSVUtil.read(termCfgFile);

        List<TermCfgItem> result = new ArrayList<>(8);
        for (CsvRow row : rows) {
            if (row.isEmpty()) {
                continue;
            }
            if (row.getFieldCount() > 1) {
                String table = row.getField(0);
                String fieldChain = row.getField(1);
                result.add(new TermCfgItem(table, fieldChain));
            }
        }
        return new TermCfg(result);
    }

    public boolean isMatch(Line line) {
        for (TermCfgItem item : items) {
            if (item.table.equals(line.table()) && item.fieldChain.equals(line.fieldChain())) {
                return true;
            }
        }
        return false;
    }

    public record TermsAndOthers(@NotNull Map<String, String> terms,
                                 @NotNull List<Line> others) {

    }

    public TermsAndOthers extractTermsAndOthers(List<Line> done) {
        Map<String, String> terms = new LinkedHashMap<>();
        List<Line> others = new ArrayList<>(128);
        for (Line line : done) {
            if (isMatch(line)) {
                terms.put(line.original(), line.translated());
            } else {
                others.add(line);
            }
        }
        return new TermsAndOthers(terms, others);
    }
}
