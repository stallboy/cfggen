package configgen.gen;

import configgen.util.CSVUtil;
import de.siegmar.fastcsv.reader.CsvRow;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class I18n {
    private Map<String, Map<String, String>> map;
    private final boolean isCRLFAsLF;
    private Map<String, String> curTable;

    public I18n() {
        isCRLFAsLF = false;
    }

    public I18n(Path path, String encoding, boolean crlfaslf) {
        map = new HashMap<>();
        List<CsvRow> rows = CSVUtil.read(path, encoding);

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("国际化i18n文件为空");
        }
        CsvRow row0 = rows.get(0);
        if (row0.getFieldCount() != 3) {
            throw new IllegalArgumentException("国际化i18n文件列数不为3");
        }

        isCRLFAsLF = crlfaslf;
        for (CsvRow row : rows) {
            if (row.isEmpty()) {
                continue;
            }
            if (row.getFieldCount() != 3) {
                System.out.println(row + " 不是3列，被忽略");
            } else {
                String table = row.getField(0);
                String raw = row.getField(1);
                String i18 = row.getField(2);
                raw = normalizeRaw(raw);

                Map<String, String> m = map.computeIfAbsent(table, _ -> new HashMap<>());
                m.put(raw, i18);
            }
        }
    }

    private String normalizeRaw(String raw) {
        if (isCRLFAsLF) {
            return raw.replaceAll("\r\n", "\n");
        } else {
            return raw;
        }
    }

    public Map<String, String> getTable(String table) {
        return map.get(table);
    }

    public String getText(Map<String, String> table, String raw) {
        if (table == null) {
            return null;
        }
        raw = normalizeRaw(raw);
        String text = table.get(raw);
        if (text != null && !text.isEmpty()) {
            return text;
        }
        return null;
    }

    public void enterTable(String tableName) {
        curTable = getTable(tableName);
    }

    public String enterText(String raw) {
        return getText(curTable, raw);
    }
}

