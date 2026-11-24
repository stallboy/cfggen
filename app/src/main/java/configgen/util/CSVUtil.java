package configgen.util;

import org.jspecify.annotations.Nullable;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.FieldMismatchStrategy;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public class CSVUtil {

    public static List<CsvRecord> read(Path path) {
        return read(path, StandardCharsets.UTF_8);
    }


    public static List<CsvRecord> read(Path path, Charset defaultEncoding) {
        try (CsvReader<CsvRecord> reader = CsvReader.builder()
                .skipEmptyLines(false)
                .commentStrategy(CommentStrategy.NONE)
                .fieldSeparator(',')
                // 4.x 默认严格(字段数不齐即抛异常)；2.x 宽松放行，readAndNormalize/ReadCsv 也按各行列数处理，故用 IGNORE
                .extraFieldStrategy(FieldMismatchStrategy.IGNORE)
                .missingFieldStrategy(FieldMismatchStrategy.IGNORE)
                // 4.x StrictCsvParser 对引号内含未转义引号等会抛异常；2.x 宽松。开启后自动切换到 RelaxedCsvParser
                .allowExtraCharsAfterClosingQuote(true)
                .ofCsvRecord(new UnicodeReader(Files.newInputStream(path), defaultEncoding))) {
            List<CsvRecord> rows = new ArrayList<>();
            for (CsvRecord csvRecord : reader) {
                rows.add(csvRecord);
            }
            return rows;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<List<String>> readAndNormalize(Path path, Charset defaultEncoding) {
        List<CsvRecord> rows = read(path, defaultEncoding);

        List<List<String>> result = new ArrayList<>(rows.size());
        OptionalInt cols = rows.stream().mapToInt(CsvRecord::getFieldCount).max();
        if (cols.isEmpty()) {
            return result;
        }
        int colCount = cols.getAsInt();
        for (CsvRecord csvRecord : rows) {
            List<String> row = new ArrayList<>(colCount);
            for (int c = 0; c < colCount; c++) {
                if (c < csvRecord.getFieldCount()) {
                    row.add(csvRecord.getField(c));
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
                sb.append(escapeCsv(row.get(c)));
                if (c != row.size() - 1) {
                    sb.append(",");
                } else {
                    sb.append("\r\n");
                }
            }
        }

    }


    public static void writeToFile(File file, List<List<String>> rows) throws IOException {
        FileUtil.writeReplacement(file.toPath(), staged -> {
            try (BomUtf8Writer writer = new BomUtf8Writer(staged)) {
                write(writer, rows);
            }
        });
    }

    public static String escapeCsv(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        // 如果包含逗号、双引号或换行符，用双引号包裹，并且双引号转义为两个双引号
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
