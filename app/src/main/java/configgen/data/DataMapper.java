package configgen.data;

import configgen.schema.*;
import java.util.*;

public class DataMapper {
    private final TableSchema tableSchema;
    private final List<ColumnInfo> columns;

    public DataMapper(TableSchema tableSchema, List<ColumnInfo> columns) {
        this.tableSchema = tableSchema;
        this.columns = columns;
    }

    public List<Object> mapDataToRow(Map<String, Object> data) {
        List<Object> row = new ArrayList<>(columns.size());

        for (ColumnInfo column : columns) {
            Object value = extractValue(data, column.getFieldName());
            row.add(formatValue(value, column));
        }

        return row;
    }

    private Object extractValue(Map<String, Object> data, String fieldPath) {
        // 实现从嵌套数据结构中提取值的逻辑
        String[] parts = fieldPath.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    private Object formatValue(Object value, ColumnInfo column) {
        // 根据映射规则格式化值
        if (value == null) {
            return "";
        }

        // 这里可以根据pack、sep等规则格式化复杂结构
        return value.toString();
    }
}