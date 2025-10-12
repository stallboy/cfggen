package configgen.data;

import configgen.ctx.HeadRow;
import configgen.ctx.HeadRows;
import configgen.util.LocaleUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static configgen.data.CfgData.DTable;
import static configgen.data.CfgData.*;

public final class HeadParser {

    public static void parse(DTable table, CfgDataStat stat, HeadRow headRow, boolean isColumnMode) {
        List<DField> header = null;
        List<String> names = null;
        DRawSheet headerSheet = null;
        if (table.rawSheets().size() > 1) {
            table.rawSheets().sort(Comparator.comparingInt(DRawSheet::index));
        }

        for (DRawSheet sheet : table.rawSheets()) {
            List<String> comments = getLogicRow(sheet, headRow.commentRow(), isColumnMode);
            List<String> curNames = getLogicRow(sheet, headRow.nameRow(), isColumnMode);
            List<String> suggestedTypes = headRow.suggestedTypeRow() >= 0 ?
                    getLogicRow(sheet, headRow.suggestedTypeRow(), isColumnMode) : Collections.emptyList();


            List<DField> h = parseFields(sheet, stat, comments, curNames, suggestedTypes);

            if (header == null) {
                names = curNames;
                header = h;
                headerSheet = sheet;
            } else if (!curNames.equals(names)) {
                throw new IllegalStateException(
                        LocaleUtil.getFormatedLocaleString("SplitDataHeaderNotEqual",
                                "{0} header: {1},\\n{2} header: {3} not equal!",
                                sheet.id(), curNames, headerSheet.id(), names));
            }
        }

        if (header != null) {
            table.fields().clear();
            table.fields().addAll(header);
        }
    }

    public static void parse(DTable table, CfgDataStat stat) {
        parse(table, stat, HeadRows.A2_Default, false);
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

    private static List<DField> parseFields(DRawSheet sheet, CfgDataStat stat,
                                            List<String> comments,
                                            List<String> names,
                                            List<String> suggestedTypes) {
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

            String comment = getComment(comments, i, name);

            String suggestedType = "";
            if (i < suggestedTypes.size()) {
                suggestedType = suggestedTypes.get(i);
            }

            DField field = new DField(name, comment, suggestedType);
            fields.add(field);
            fieldIndices.add(i);
        }


        sheet.fieldIndices().clear();
        sheet.fieldIndices().addAll(fieldIndices);
        return fields;
    }

    private static String getComment(List<String> comments, int i, String name) {
        String comment = "";
        if (i < comments.size()) {
            comment = comments.get(i);
            if (comment == null) {
                comment = "";
            } else {
                comment = comment.replaceAll("\r\n|\r|\n", "--");
                if (i == 0 && comment.startsWith("#")) { // 第一列是注释列
                    comment = comment.substring(1);
                }
                comment = comment.trim();
            }
        }
        if (comment.equalsIgnoreCase(name)) { // 忽略重复信息
            comment = "";
        }
        return comment;
    }

    private static String getColumnName(String name) {
        int i = name.indexOf('.'); // 给机会在.后面来声明此bean下第一个字段的名称
        if (i != -1) {
            return name.substring(0, i).trim();
        }

        i = name.indexOf(','); // 给机会在,后面来声明此bean下第一个字段的名称
        if (i != -1) {
            return name.substring(0, i).trim();
        }

        i = name.indexOf('@'); //为了是兼容之前版本
        if (i != -1) {
            return name.substring(0, i).trim();
        }
        return name.trim();
    }

}
