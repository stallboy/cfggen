package configgen.data;

import configgen.schema.CfgSchema;
import configgen.schema.TableSchema;
import configgen.util.LocaleUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static configgen.data.CfgData.DTable;
import static configgen.data.CfgData.*;

final class HeadParser {
    static void parse(DTable table, DataStat stat, CfgSchema cfgSchema) {
        parse(table, stat, isColumnMode(table, cfgSchema));
    }

    static boolean isColumnMode(DTable table, CfgSchema cfgSchema) {
        boolean isColumnMode = false;
        if (cfgSchema != null) {
            cfgSchema.requireResolved();
            String name = table.tableName();
            TableSchema schema = cfgSchema.findTable(name);
            if (schema != null) {
                isColumnMode = schema.isColumnMode();
            }
        }
        return isColumnMode;
    }

    static void parse(DTable table, DataStat stat, boolean isColumnMode) {
        List<DField> header = null;
        List<String> names = null;
        DRawSheet headerSheet = null;
        if (table.rawSheets().size() > 1) {
            table.rawSheets().sort(Comparator.comparingInt(DRawSheet::index));
        }
        for (DRawSheet sheet : table.rawSheets()) {
            List<String> comments = getLogicRow(sheet, 0, isColumnMode);
            List<String> curNames = getLogicRow(sheet, 1, isColumnMode);
            List<DField> h = parse(sheet, stat, comments, curNames);

            if (header == null) {
                names = curNames;
                header = h;
                headerSheet = sheet;
            } else if (!curNames.equals(names)) {
                throw new IllegalStateException(
                        LocaleUtil.getMessage("SplitDataHeaderNotEqual",
                                sheet.id(), curNames, headerSheet.id(), names));
            }
        }

        if (header != null) {
            table.fields().clear();
            table.fields().addAll(header);
        }
    }

    private static List<String> getLogicRow(DRawSheet sheet, int rowIndex, boolean isColumnMode) {
        List<String> result = new ArrayList<>();
        if (!isColumnMode) {
            if (rowIndex < sheet.rows().size()) {
                DRawRow row = sheet.rows().get(rowIndex);
                for (int i = 0; i < row.count(); i++) {
                    result.add(row.cell(i));
                }
            }

        } else {
            for (DRawRow row : sheet.rows()) {
                String c = row.cell(rowIndex);
                result.add(c);
            }
        }

        //trim掉空的后缀
        int i = result.size() - 1;
        for (; i >= 0; i--) {
            if (!result.get(i).trim().isEmpty()) {
                break;
            }
        }
        if (i == result.size() - 1) {
            return result;
        } else {
            return result.subList(0, i + 1);
        }
    }

    private static List<DField> parse(DRawSheet sheet, DataStat stat, List<String> comments, List<String> names) {
        List<DField> fields = new ArrayList<>();
        int size = names.size();
        List<Integer> fieldIndices = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String name = names.get(i);
            if (name == null) {
                stat.ignoredColumnCount++;
                continue;
            }
            name = getColumnName(name);
            if (name.isEmpty()) {
                stat.ignoredColumnCount++;
                continue;
            }
            stat.columnCount++;

            String comment = "";
            if (i < comments.size()) {
                comment = comments.get(i);
                if (comment == null) {
                    comment = "";
                } else {
                    comment = getComment(comment);
                }
            }
            if (comment.equalsIgnoreCase(name)) { // 忽略重复信息
                comment = "";
            }
            DField field = new DField(name, comment);
            fields.add(field);
            fieldIndices.add(i);
        }


        sheet.fieldIndices().clear();
        sheet.fieldIndices().addAll(fieldIndices);
        return fields;
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
        return comment.replaceAll("\r\n|\r|\n", "--");
    }


}
