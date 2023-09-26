package configgen.data;

import de.siegmar.fastcsv.reader.CsvRow;
import org.apache.poi.ss.usermodel.Sheet;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.Row;

import java.util.*;

import static configgen.data.CfgData.*;

public record TableDataHeader(List<HeaderField> fields) {

    public record HeaderField(String name,
                              String comment,
                              int index) {
    }

    public static TableDataHeader of(TableData tableData, boolean isColumnMode) {
        TableDataHeader header = null;
        SheetData headerSheet = null;
        for (SheetData sheet : tableData.sheets()) {
            TableDataHeader h = of(sheet, isColumnMode);

            if (header == null) {
                header = h;
                headerSheet = sheet;
            } else if (!h.equals(header)) {
                throw new IllegalStateException(STR. "\{ sheet.id() } 文件头和 \{ headerSheet.id() }文件头不匹配！" );
            }
        }
        return header;
    }

    public static TableDataHeader of(SheetData sheetData, boolean isColumnMode) {
        List<String> rawCommentHeader = new ArrayList<>();
        List<String> rawNameHeader = new ArrayList<>();

        if (isColumnMode) {
            switch (sheetData) {
                case CsvData csvData -> {
                    for (CsvRow row : csvData.rows()) {
                        String c = "";
                        String name = "";
                        if (row.getFieldCount() > 0) {
                            c = row.getField(0).trim();
                        }
                        if (row.getFieldCount() > 1) {
                            name = row.getField(1).trim();
                        }
                        rawCommentHeader.add(c);
                        rawNameHeader.add(name);
                    }
                }
                case ExcelSheetData excelSheetData -> {
                    for (Row row : excelSheetData.rows()) {
                        String c = "";
                        String name = "";
                        Optional<String> cell = row.getCellAsString(0);
                        if (cell.isPresent()) {
                            c = cell.get();
                        }
                        cell = row.getCellAsString(1);
                        if (cell.isPresent()) {
                            name = cell.get();
                        }
                        rawCommentHeader.add(c);
                        rawNameHeader.add(name);
                    }
                }
            }

        } else {
            switch (sheetData) {
                case CsvData csvData -> {
                    List<CsvRow> rows = csvData.rows();
                    if (!rows.isEmpty()) {
                        for (String f : rows.get(0).getFields()) {
                            rawCommentHeader.add(f.trim());
                        }
                    }
                    if (rows.size() > 1) {
                        for (String f : rows.get(1).getFields()) {
                            rawNameHeader.add(f.trim());
                        }
                    }
                }
                case ExcelSheetData excelSheetData -> {
                    List<Row> rows = excelSheetData.rows();
                    if (!rows.isEmpty()) {
                        for (Cell s : rows.get(0)) {
                            if (s != null) {
                                rawCommentHeader.add(s.getText().trim());
                            } else {
                                rawCommentHeader.add("");
                            }
                        }
                    }

                    if (rows.size() > 1) {
                        for (Cell s : rows.get(1)) {
                            if (s != null) {
                                rawNameHeader.add(s.getText().trim());
                            } else {
                                rawNameHeader.add("");
                            }
                        }
                    }
                }
            }
        }
        return of(rawCommentHeader, rawNameHeader);
    }

    public static TableDataHeader of(List<String> rawCommentHeader, List<String> rawNameHeader) {
        Objects.requireNonNull(rawCommentHeader);
        Objects.requireNonNull(rawNameHeader);

        List<HeaderField> fields = new ArrayList<>();
        int size = rawNameHeader.size();
        for (int i = 0; i < size; i++) {
            String name = rawNameHeader.get(i);
            if (name == null) {
                continue;
            }
            name = getColumnName(name);
            if (name.isEmpty()) {
                continue;
            }

            String comment = "";
            if (i < rawCommentHeader.size()) {
                comment = rawCommentHeader.get(i);
                if (comment == null) {
                    comment = "";
                } else {
                    comment = getComment(comment);
                }
            }
            if (comment.equalsIgnoreCase(name)) { // 忽略重复信息
                comment = "";
            }
            HeaderField field = new HeaderField(name, comment, i);
            fields.add(field);
        }
        return new TableDataHeader(fields);
    }

    private static String getColumnName(String name) {
        int i = name.indexOf(','); // 给机会在,后面来声明此bean下第一个字段的名称，其实用desc行也可以声明。
        if (i != -1) {
            return name.substring(0, i).trim();
        } else {
            int j = name.indexOf('@'); //为了是兼容之前版本
            if (j != -1) {
                return name.substring(0, j).trim();
            } else {
                return name.trim();
            }
        }
    }

    private static String getComment(String comment) {
        return comment.replaceAll("\r\n|\r|\n", "-");
    }
}
