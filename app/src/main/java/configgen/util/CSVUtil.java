package configgen.util;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CSVUtil {

    public static List<CsvRow> read(Path path, String defaultEncoding) {
        try (CsvReader reader = CsvReader.builder().build(new UnicodeReader(Files.newInputStream(path), defaultEncoding))) {
            List<CsvRow> rows = new ArrayList<>();
            for (CsvRow csvRow : reader) {
                rows.add(csvRow);
            }
            return rows;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    public static void write(UTF8Writer writer, List<List<String>> rows)  {
        if (rows.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder(256);
        write(sb, rows, "");
        writer.write(sb.toString());
    }

    public static void write(StringBuilder sb, List<List<String>> rows, String prefix) {
        int columnCount = rows.getFirst().size();

        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (row.size() != columnCount) {
                throw new IllegalArgumentException("csv里每行数据个数应该相同，但这里第" + r + "行，数据有" + row.size() + "个,跟第一行" + columnCount + ",个数不符合");
            }

            sb.append(prefix);
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
        try (UTF8Writer w = new UTF8Writer(Files.newOutputStream(file.toPath()))) {
            write(w, rows);
        }
    }

}
