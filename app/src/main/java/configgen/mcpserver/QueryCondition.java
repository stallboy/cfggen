package configgen.mcpserver;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import configgen.value.CfgValue.VStruct;
import configgen.value.ValueUtil;
import configgen.schema.TableSchema;
import configgen.util.Logger;

import java.util.*;

public class QueryCondition {
    private final Map<String, Object> conditions;
    private final int limit;
    private final int offset;
    private final String sortField;
    private final boolean sortAscending;

    public QueryCondition(Map<String, Object> conditions, int limit, int offset,
                         String sortField, boolean sortAscending) {
        this.conditions = conditions != null ? conditions : Map.of();
        this.limit = limit > 0 ? limit : 100; // 默认限制100条
        this.offset = Math.max(offset, 0);
        this.sortField = sortField;
        this.sortAscending = sortAscending;
    }

    public boolean matches(VStruct record) {
        if (conditions.isEmpty()) {
            return true;
        }

        TableSchema tableSchema = (TableSchema) record.schema();

        for (Map.Entry<String, Object> condition : conditions.entrySet()) {
            String fieldName = condition.getKey();
            Object expectedValue = condition.getValue();

            Object actualValue = extractFieldValue(record, tableSchema, fieldName);

            if (!matchesValue(actualValue, expectedValue)) {
                return false;
            }
        }

        return true;
    }

    private Object extractFieldValue(VStruct record, TableSchema tableSchema, String fieldName) {
        try {
            // 尝试从记录中提取字段值
            // 这里需要根据实际的VStruct结构来实现字段值提取
            // 暂时使用简单的字符串匹配作为占位实现
            String recordStr = record.packStr();
            return recordStr;
        } catch (Exception e) {
            Logger.log("Error extracting field value for " + fieldName + ": " + e.getMessage());
            return null;
        }
    }

    private boolean matchesValue(Object actualValue, Object expectedValue) {
        if (actualValue == null && expectedValue == null) {
            return true;
        }
        if (actualValue == null || expectedValue == null) {
            return false;
        }

        String actualStr = actualValue.toString().toLowerCase();
        String expectedStr = expectedValue.toString().toLowerCase();

        // 支持简单的字符串包含匹配
        return actualStr.contains(expectedStr);
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public String getSortField() {
        return sortField;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }

    public Map<String, Object> getConditions() {
        return conditions;
    }

    public static QueryCondition parse(String conditionStr) {
        if (conditionStr == null || conditionStr.trim().isEmpty()) {
            return new QueryCondition(Map.of(), 100, 0, null, true);
        }

        try {
            JSONObject jsonObject = JSON.parseObject(conditionStr);

            // 解析查询条件
            Map<String, Object> conditions = new HashMap<>();
            if (jsonObject.containsKey("where")) {
                JSONObject whereObj = jsonObject.getJSONObject("where");
                if (whereObj != null) {
                    for (String key : whereObj.keySet()) {
                        conditions.put(key, whereObj.get(key));
                    }
                }
            }

            // 解析分页参数
            int limit = jsonObject.getIntValue("limit", 100);
            int offset = jsonObject.getIntValue("offset", 0);

            // 解析排序参数
            String sortField = jsonObject.getString("sort");
            boolean sortAscending = jsonObject.getBooleanValue("asc", true);

            return new QueryCondition(conditions, limit, offset, sortField, sortAscending);
        } catch (Exception e) {
            Logger.log("Error parsing query condition: " + e.getMessage());
            // 如果JSON解析失败，尝试作为简单字符串条件处理
            Map<String, Object> simpleCondition = Map.of("_search", conditionStr);
            return new QueryCondition(simpleCondition, 100, 0, null, true);
        }
    }

    public static QueryCondition createSimple(String searchText) {
        Map<String, Object> conditions = Map.of("_search", searchText);
        return new QueryCondition(conditions, 100, 0, null, true);
    }
}