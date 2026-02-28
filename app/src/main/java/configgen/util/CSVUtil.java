package configgen.util;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public class CSVUtil {

    public static List<CsvRow> read(Path path) {
        return read(path, "UTF-8", ',');
    }


    public static List<CsvRow> read(Path path, String defaultEncoding) {
        return read(path, defaultEncoding, ',');
    }

    public static List<CsvRow> read(Path path, String defaultEncoding, char fieldSeparator) {
        try (CsvReader reader = CsvReader.builder()
                .skipEmptyRows(false)
                .commentStrategy(CommentStrategy.NONE)
                .fieldSeparator(fieldSeparator)
                .build(new UnicodeReader(Files.newInputStream(path), Charset.forName(defaultEncoding)))) {
            List<CsvRow> rows = new ArrayList<>();
            for (CsvRow csvRow : reader) {
                rows.add(csvRow);
            }
            return rows;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<List<String>> readAndNormalize(Path path, String defaultEncoding) {
        List<CsvRow> rows = read(path, defaultEncoding);

        List<List<String>> result = new ArrayList<>(rows.size());
        OptionalInt cols = rows.stream().mapToInt(CsvRow::getFieldCount).max();
        if (cols.isEmpty()) {
            return result;
        }
        int colCount = cols.getAsInt();
        for (CsvRow csvRow : rows) {
            List<String> row = new ArrayList<>(colCount);
            for (int c = 0; c < colCount; c++) {
                if (c < csvRow.getFieldCount()) {
                    row.add(csvRow.getField(c));
                } else {
                    row.add("");
                }
            }
            result.add(row);
        }
        return result;
    }


    /* https://tools.ietf.org/html/rfc4180
   6.  Fields containing line breaks (CRLF), double quotes, and commas
       should be enclosed in double-quotes.  For example:

       "aaa","b CRLF
       bb","ccc" CRLF
       zzz,yyy,xxx

   7.  If double-quotes are used to enclose fields, then a double-quote
       appearing inside a field must be escaped by preceding it with
       another double quote.  For example:

       "aaa","b""bb","ccc"
       */
    public static void write(BomUtf8Writer writer, List<List<String>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder(256);
        write(sb, rows);
        writer.write(sb.toString());
    }

    public static void write(StringBuilder sb, List<List<String>> rows) {
        int columnCount = rows.getFirst().size();

        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (row.size() != columnCount) {
                throw new IllegalArgumentException(LocaleUtil.getFormatedLocaleString("CsvUtil.RowColumnCountMismatch",
                        "CSV row {0} has {1} columns, but first row has {2} columns",
                        r, row.size(), columnCount));
            }

            for (int c = 0; c < row.size(); c++) {
                String cell = row.get(c);
                boolean enclose = false;
                if (cell.contains("\"")) {
                    cell = cell.replace("\"", "\"\"");
                    enclose = true;
                } else if (cell.contains("\r\n") || cell.contains(",")) {
                    enclose = true;
                } else if (cell.contains("\r") || cell.contains("\n")) { //这个是为了兼容excel，不是rfc4180的要求
                    enclose = true;
                }

                if (enclose) {
                    cell = "\"" + cell + "\"";
                }


                sb.append(cell);
                if (c != row.size() - 1) {
                    sb.append(",");
                } else {
                    sb.append("\r\n");
                }
            }
        }

    }


    public static void writeToFile(File file, List<List<String>> rows) throws IOException {
        try (BomUtf8Writer w = new BomUtf8Writer(file.toPath())) {
            write(w, rows);
        }
    }

    public static String escapeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        // 如果包含逗号、双引号或换行符，用双引号包裹，并且双引号转义为两个双引号
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
