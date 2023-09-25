package configgen.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record DataHeader(List<HeaderField> fields) {

    public record HeaderField(String name,
                              String comment,
                              int index) {
    }

    public static DataHeader of(List<String> commentHeader, List<String> nameHeader) {
        Objects.requireNonNull(commentHeader);
        Objects.requireNonNull(nameHeader);

        List<HeaderField> fields = new ArrayList<>();
        int size = nameHeader.size();
        for (int i = 0; i < size; i++) {
            String name = nameHeader.get(i);
            if (name == null) {
                continue;
            }
            name = getColumnName(name);
            if (name.isEmpty()) {
                continue;
            }

            String comment = "";
            if (i < commentHeader.size()) {
                comment = commentHeader.get(i);
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
        return new DataHeader(fields);
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
        return comment.replaceAll("\r\n|\r|\n", "$");
    }
}
