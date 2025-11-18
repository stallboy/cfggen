package configgen.mcpserver.services;

import configgen.mcpserver.QueryCondition;
import configgen.mcpserver.models.McpResponse;
import configgen.schema.TableSchema;
import configgen.value.CfgValue;
import configgen.value.CfgValue.VStruct;
import configgen.value.CfgValue.VTable;
import configgen.value.ValueUtil;
import configgen.value.CfgValue.Value;
import configgen.editorserver.RecordService;
import configgen.util.Logger;

import java.util.*;

/**
 * 数据查询服务
 */
public class DataQueryService {
    private final CfgValue cfgValue;

    public DataQueryService(CfgValue cfgValue) {
        this.cfgValue = cfgValue;
    }

    /**
     * 查询数据
     * @param table 表名
     * @param condition 查询条件
     * @param requestId 请求ID
     * @return 查询结果
     */
    public Map<String, Object> queryData(String table, String condition, String requestId) {
        if (table == null || table.isEmpty()) {
            return McpResponse.error(400, "Table name is required", requestId).toMap();
        }

        VTable vTable = cfgValue.vTableMap().get(table);
        if (vTable == null) {
            return McpResponse.error(400, "Table not found: " + table, requestId).toMap();
        }

        try {
            // 解析查询条件
            QueryCondition queryCondition = QueryCondition.parse(condition);

            // 过滤记录
            List<VStruct> filteredRecords = filterRecordsWithCondition(vTable, queryCondition);

            // 应用排序
            List<VStruct> sortedRecords = sortRecords(filteredRecords, queryCondition);

            // 应用分页
            List<VStruct> paginatedRecords = applyPagination(sortedRecords, queryCondition);

            Map<String, Object> data = formatDataResponse(table, paginatedRecords, queryCondition, filteredRecords.size());
            return McpResponse.success(data, requestId).toMap();
        } catch (Exception e) {
            Logger.log("Error querying data: " + e.getMessage());
            return McpResponse.error(500, "Error querying data: " + e.getMessage(), requestId).toMap();
        }
    }

    /**
     * 根据查询条件过滤记录
     */
    private List<VStruct> filterRecordsWithCondition(VTable vTable, QueryCondition queryCondition) {
        List<VStruct> allRecords = vTable.valueList();

        if (queryCondition.getConditions().isEmpty()) {
            return allRecords;
        }

        List<VStruct> filtered = new ArrayList<>();
        for (VStruct record : allRecords) {
            if (queryCondition.matches(record)) {
                filtered.add(record);
            }
        }
        return filtered;
    }

    /**
     * 对记录进行排序
     */
    private List<VStruct> sortRecords(List<VStruct> records, QueryCondition queryCondition) {
        if (queryCondition.getSortField() == null || records.isEmpty()) {
            return records;
        }

        List<VStruct> sorted = new ArrayList<>(records);
        sorted.sort((r1, r2) -> {
            try {
                TableSchema tableSchema = (TableSchema) r1.schema();
                Object value1 = extractFieldValue(r1, tableSchema, queryCondition.getSortField());
                Object value2 = extractFieldValue(r2, tableSchema, queryCondition.getSortField());

                int comparison = compareValues(value1, value2);
                return queryCondition.isSortAscending() ? comparison : -comparison;
            } catch (Exception e) {
                Logger.log("Error sorting records: " + e.getMessage());
                return 0;
            }
        });

        return sorted;
    }

    /**
     * 应用分页
     */
    private List<VStruct> applyPagination(List<VStruct> records, QueryCondition queryCondition) {
        int offset = queryCondition.getOffset();
        int limit = queryCondition.getLimit();

        if (offset >= records.size()) {
            return List.of();
        }

        int endIndex = Math.min(offset + limit, records.size());
        return records.subList(offset, endIndex);
    }

    /**
     * 提取字段值
     */
    private Object extractFieldValue(VStruct record, TableSchema tableSchema, String fieldName) {
        try {
            // 这里需要根据实际的VStruct结构来实现字段值提取
            // 暂时使用简单的字符串匹配作为占位实现
            String recordStr = record.packStr();
            return recordStr;
        } catch (Exception e) {
            Logger.log("Error extracting field value for " + fieldName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * 比较值
     */
    private int compareValues(Object value1, Object value2) {
        if (value1 == null && value2 == null) return 0;
        if (value1 == null) return -1;
        if (value2 == null) return 1;

        String str1 = value1.toString();
        String str2 = value2.toString();

        // 尝试数值比较
        try {
            Double num1 = Double.parseDouble(str1);
            Double num2 = Double.parseDouble(str2);
            return num1.compareTo(num2);
        } catch (NumberFormatException e) {
            // 如果无法解析为数字，使用字符串比较
            return str1.compareTo(str2);
        }
    }

    /**
     * 格式化数据查询响应
     */
    private Map<String, Object> formatDataResponse(String table, List<VStruct> records,
                                                  QueryCondition queryCondition, int totalCount) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("table", table);
        response.put("count", records.size());
        response.put("totalCount", totalCount);
        response.put("limit", queryCondition.getLimit());
        response.put("offset", queryCondition.getOffset());

        // 添加查询条件信息
        if (!queryCondition.getConditions().isEmpty()) {
            response.put("conditions", queryCondition.getConditions());
        }
        if (queryCondition.getSortField() != null) {
            response.put("sort", Map.of(
                "field", queryCondition.getSortField(),
                "ascending", queryCondition.isSortAscending()
            ));
        }

        List<Map<String, Object>> recordList = new ArrayList<>();
        for (VStruct record : records) {
            TableSchema tableSchema = (TableSchema) record.schema();
            Value pkValue = ValueUtil.extractPrimaryKeyValue(record, tableSchema);
            String id = pkValue.packStr();
            String title = RecordService.getBriefTitle(record);

            recordList.add(Map.of(
                "id", id,
                "title", title,
                "value", record.packStr()
            ));
        }
        response.put("records", recordList);

        return response;
    }
}