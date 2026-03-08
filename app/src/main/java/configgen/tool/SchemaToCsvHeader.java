package configgen.tool;

import configgen.schema.*;

import java.util.ArrayList;
import java.util.List;

class SchemaToCsvHeader {
    private final List<String> commentRow = new ArrayList<>();
    private final List<String> nameRow = new ArrayList<>();
    private boolean isFirstOfField = true;
    private String topLevelName = "";
    private String topLevelComment = "";

    void flattenFields(List<FieldSchema> fields) {
        for (FieldSchema field : fields) {
            isFirstOfField = true;
            topLevelName = field.name();
            topLevelComment = field.comment() != null ? field.comment() : "";
            flattenField(field);
        }
    }

    private void flattenField(FieldSchema field) {
        int span = Span.fieldSpan(field);
        String name = field.name();
        // isFirstOfField 时用顶层 comment，否则用字段自己的
        String comment = isFirstOfField ? topLevelComment : (field.comment() != null ? field.comment() : "");

        if (span == 1) {
            // 占 1 列
            // 顶层字段的第一列用 name，嵌套字段的第一列用 topLevelName._name
            String colName;
            if (isFirstOfField) {
                colName = name.equals(topLevelName) ? name : topLevelName + "._" + name;
            } else {
                colName = "_" + name;
            }
            addColumn(colName, comment);
            return;
        }

        // span > 1 的情况
        // 注意：PACK 或 Sep 格式时 span == 1，已在前面处理并 return
        switch (field.type()) {
            case FieldType.StructRef structRef -> {
                flattenFieldable(structRef.obj());
            }
            case FieldType.FList fList -> {
                int count = getCount(field.fmt());
                for (int i = 1; i <= count; i++) {
                    // 只有第一个元素加 comment（使用顶层 comment）
                    String itemComment = isFirstOfField ? topLevelComment : "";
                    flattenSimpleType(fList.item(), String.valueOf(i), itemComment);
                }
            }
            case FieldType.FMap fMap -> {
                int count = getCount(field.fmt());
                for (int i = 1; i <= count; i++) {
                    // 只有第一个 key 加 comment（使用顶层 comment）
                    String keyComment = isFirstOfField ? topLevelComment : "";
                    flattenSimpleType(fMap.key(), "k" + i, keyComment);
                    flattenSimpleType(fMap.value(), "v" + i, "");
                }
            }
            default -> { }
        }
    }

    // 抽取公共逻辑：展开 Fieldable (Struct 或 Interface)
    private void flattenFieldable(Fieldable obj) {
        if (obj instanceof StructSchema ss) {
            for (FieldSchema sub : ss.fields()) {
                flattenField(sub);
            }
        } else if (obj instanceof InterfaceSchema is) {
            flattenInterface(is);
        }
    }

    private void flattenInterface(InterfaceSchema is) {
        // 类型列
        String typeColName = isFirstOfField ? topLevelName + "._type" : "_type";
        String typeComment = isFirstOfField ? topLevelComment : "";
        addColumn(typeColName, typeComment);

        // 使用通用 _p1, _p2, ... 命名（数量 = Span.span(is) - 1，去掉 _type 列）
        int dataSpan = Span.span(is) - 1;
        for (int i = 1; i <= dataSpan; i++) {
            addColumn("_p" + i, "");
        }
    }

    private void flattenSimpleType(FieldType.SimpleType type, String suffix, String comment) {
        int span = Span.simpleTypeSpan(type);

        if (span == 1) {
            // 占 1 列
            String colName = isFirstOfField ? topLevelName + "._" + suffix : "_" + suffix;
            addColumn(colName, comment);
            return;
        }

        // span > 1 的情况，递归处理
        switch (type) {
            case FieldType.StructRef structRef -> {
                flattenFieldable(structRef.obj());
            }
            default -> { } // Primitive 类型的 span 总是 1，不会进入这里
        }
    }

    private int getCount(FieldFormat fmt) {
        if (fmt instanceof FieldFormat.Fix fix) return fix.count();
        if (fmt instanceof FieldFormat.Block block) return block.fix();
        return 0;
    }

    private void addColumn(String name, String comment) {
        nameRow.add(name);
        commentRow.add(comment);
        isFirstOfField = false;
    }

    List<String> getCommentRow() { return commentRow; }
    List<String> getNameRow() { return nameRow; }
}
