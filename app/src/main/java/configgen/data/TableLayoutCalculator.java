package configgen.data;

import configgen.schema.*;
import java.util.*;

public class TableLayoutCalculator {
    private final TableSchema tableSchema;

    public TableLayoutCalculator(TableSchema tableSchema) {
        this.tableSchema = tableSchema;
    }

    public List<ColumnInfo> calculateLayout() {
        List<ColumnInfo> columns = new ArrayList<>();

        for (FieldSchema field : tableSchema.fields()) {
            calculateFieldLayout(field, field.name(), columns);
        }

        return columns;
    }

    private void calculateFieldLayout(FieldSchema field, String prefix, List<ColumnInfo> columns) {
        FieldType fieldType = field.type();
        String fieldName = prefix.isEmpty() ? field.name() : prefix;

        switch (fieldType) {
            case FieldType.Primitive primitive -> {
                // 基本类型：占1列
                columns.add(new ColumnInfo(fieldName, fieldName, 1));
            }
            case FieldType.StructRef structRef -> {
                // 结构体引用：根据映射规则计算列数
                Fieldable obj = structRef.obj();
                if (obj instanceof StructSchema structSchema) {
                    calculateStructLayout(structSchema, fieldName, columns, field);
                } else {
                    // 如果无法获取结构体定义，使用默认处理
                    columns.add(new ColumnInfo(fieldName, fieldName, 1));
                }
            }
            case FieldType.FList fList -> {
                // 列表类型：根据fix或block规则计算列数
                calculateListLayout(fList, fieldName, columns, field);
            }
            case FieldType.FMap fMap -> {
                // 映射类型：根据fix或block规则计算列数
                calculateMapLayout(fMap, fieldName, columns, field);
            }
        }
    }

    private void calculateStructLayout(StructSchema struct, String prefix,
                                     List<ColumnInfo> columns, FieldSchema field) {
        // 检查是否有pack、sep等映射规则
        if (hasPackMapping(field)) {
            // pack规则：整个结构压缩到1列
            columns.add(new ColumnInfo(prefix, prefix, 1));
        } else if (hasSepMapping(field)) {
            // sep规则：整个结构压缩到1列，使用自定义分隔符
            columns.add(new ColumnInfo(prefix, prefix, 1));
        } else {
            // auto规则：递归计算每个字段
            for (FieldSchema structField : struct.fields()) {
                calculateFieldLayout(structField, prefix + "." + structField.name(), columns);
            }
        }
    }

    private void calculateListLayout(FieldType.FList fList, String prefix,
                                   List<ColumnInfo> columns, FieldSchema field) {
        // 检查fix、block、pack等映射规则
        if (hasFixMapping(field)) {
            int fixCount = getFixCount(field);
            // fix规则：固定列数 = 元素类型占格 × count
            for (int i = 0; i < fixCount; i++) {
                // 为列表元素创建临时字段定义
                FieldSchema tempField = new FieldSchema(prefix + "[" + i + "]", fList.item(),
                    configgen.schema.FieldFormat.AutoOrPack.AUTO, configgen.schema.Metadata.of());
                calculateFieldLayout(tempField, prefix + "[" + i + "]", columns);
            }
        } else if (hasBlockMapping(field)) {
            // block规则：横向固定，纵向扩展
            int blockSize = getBlockSize(field);
            for (int i = 0; i < blockSize; i++) {
                FieldSchema tempField = new FieldSchema(prefix + "[" + i + "]", fList.item(),
                    configgen.schema.FieldFormat.AutoOrPack.AUTO, configgen.schema.Metadata.of());
                calculateFieldLayout(tempField, prefix + "[" + i + "]", columns);
            }
        } else if (hasPackMapping(field)) {
            // pack规则：整个列表压缩到1列
            columns.add(new ColumnInfo(prefix, prefix, 1));
        } else {
            // 默认：递归计算元素类型
            FieldSchema tempField = new FieldSchema(prefix + "[0]", fList.item(),
                configgen.schema.FieldFormat.AutoOrPack.AUTO, configgen.schema.Metadata.of());
            calculateFieldLayout(tempField, prefix + "[0]", columns);
        }
    }

    private void calculateMapLayout(FieldType.FMap fMap, String prefix,
                                  List<ColumnInfo> columns, FieldSchema field) {
        // 映射类型的布局计算（类似列表）
        calculateListLayout(new FieldType.FList(fMap.value()), prefix, columns, field);
    }

    // 辅助方法：检查映射规则
    private boolean hasPackMapping(FieldSchema field) {
        return field.meta().hasTag("pack");
    }

    private boolean hasSepMapping(FieldSchema field) {
        return field.meta().hasTag("sep");
    }

    private boolean hasFixMapping(FieldSchema field) {
        return field.meta().get("fix") != null;
    }

    private boolean hasBlockMapping(FieldSchema field) {
        return field.meta().get("block") != null;
    }

    private int getFixCount(FieldSchema field) {
        configgen.schema.Metadata.MetaValue fixValue = field.meta().get("fix");
        if (fixValue instanceof configgen.schema.Metadata.MetaInt metaInt) {
            return metaInt.value();
        }
        return 0;
    }

    private int getBlockSize(FieldSchema field) {
        configgen.schema.Metadata.MetaValue blockValue = field.meta().get("block");
        if (blockValue instanceof configgen.schema.Metadata.MetaInt metaInt) {
            return metaInt.value();
        }
        return 0;
    }
}

class ColumnInfo {
    private final String fieldName;
    private final String header;
    private final int columnSpan;

    public ColumnInfo(String fieldName, String header, int columnSpan) {
        this.fieldName = fieldName;
        this.header = header;
        this.columnSpan = columnSpan;
    }

    public String getFieldName() { return fieldName; }
    public String getHeader() { return header; }
    public int getColumnSpan() { return columnSpan; }
}