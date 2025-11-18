package configgen.mcpserver.services;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import configgen.mcpserver.CascadeForeignKeyProcessor;
import configgen.mcpserver.models.McpResponse;
import configgen.ctx.Context;
import configgen.editorserver.RecordEditService;
import configgen.editorserver.SchemaService;
import configgen.schema.TableSchema;
import configgen.schema.ForeignKeySchema;
import configgen.value.CfgValue;
import configgen.value.CfgValue.VStruct;
import configgen.value.CfgValue.VTable;
import configgen.value.ValueUtil;
import configgen.value.CfgValue.Value;
import configgen.editorserver.RecordService;
import configgen.util.Logger;

import java.util.*;

/**
 * 数据更新服务
 */
public class DataUpdateService {
    private final Context context;
    private final CfgValue cfgValue;

    public DataUpdateService(Context context, CfgValue cfgValue) {
        this.context = context;
        this.cfgValue = cfgValue;
    }

    /**
     * 更新数据
     * @param table 表名
     * @param id 记录ID
     * @param newValue 新值
     * @param requestId 请求ID
     * @return 更新结果
     */
    public Map<String, Object> updateData(String table, String id, String newValue, String requestId) {
        if (table == null || table.isEmpty()) {
            return McpResponse.error(400, "Table name is required", requestId).toMap();
        }

        if (id == null || id.isEmpty()) {
            return McpResponse.error(400, "Record ID is required", requestId).toMap();
        }

        if (newValue == null || newValue.isEmpty()) {
            return McpResponse.error(400, "New value is required", requestId).toMap();
        }

        try {
            // 解析新值数据
            JSONObject data = JSON.parseObject(newValue);
            if (data == null) {
                return McpResponse.error(400, "Invalid JSON data", requestId).toMap();
            }

            // 处理级联外键
            CascadeForeignKeyProcessor cascadeProcessor = new CascadeForeignKeyProcessor(context, cfgValue);
            List<CascadeForeignKeyProcessor.CascadeRecord> cascadeRecords =
                cascadeProcessor.processCascadeForeignKeys(table, data);

            // 创建级联记录
            Map<String, JSONObject> cascadeData =
                cascadeProcessor.createCascadeRecords(cascadeRecords, data);

            // 验证级联记录
            if (!cascadeProcessor.validateCascadeRecords(cascadeData)) {
                return McpResponse.error(400, "Invalid cascade foreign key records", requestId).toMap();
            }

            // 先创建级联记录
            Map<String, RecordEditService.RecordEditResult> cascadeResults = new HashMap<>();
            for (Map.Entry<String, JSONObject> entry : cascadeData.entrySet()) {
                String cascadeTable = entry.getKey();
                JSONObject cascadeRecord = entry.getValue();

                RecordEditService editService = new RecordEditService(cfgValue, context);
                RecordEditService.RecordEditResult cascadeResult =
                    editService.addOrUpdateRecord(cascadeTable, cascadeRecord.toJSONString());
                cascadeResults.put(cascadeTable, cascadeResult);
            }

            // 再更新主记录
            RecordEditService mainEditService = new RecordEditService(cfgValue, context);
            RecordEditService.RecordEditResult result = mainEditService.addOrUpdateRecord(table, newValue);
            Map<String, Object> responseData = formatEditResponse(result, cascadeResults);
            return McpResponse.success(responseData, requestId).toMap();
        } catch (Exception e) {
            Logger.log("Error updating data: " + e.getMessage());
            return McpResponse.error(500, "Error updating data: " + e.getMessage(), requestId).toMap();
        }
    }

    /**
     * 删除数据
     * @param table 表名
     * @param id 记录ID
     * @param requestId 请求ID
     * @return 删除结果
     */
    public Map<String, Object> removeData(String table, String id, String requestId) {
        if (table == null || table.isEmpty()) {
            return McpResponse.error(400, "Table name is required", requestId).toMap();
        }

        if (id == null || id.isEmpty()) {
            return McpResponse.error(400, "Record ID is required", requestId).toMap();
        }

        try {
            // 检查外键引用
            List<ForeignKeyReference> references = checkForeignKeyReferences(table, id);
            if (!references.isEmpty()) {
                Map<String, Object> errorData = Map.of(
                    "table", table,
                    "id", id,
                    "foreignKeyReferences", formatForeignKeyReferences(references)
                );
                return McpResponse.error(409, "Cannot delete record due to foreign key references", requestId, errorData).toMap();
            }

            RecordEditService editService = new RecordEditService(cfgValue, context);
            RecordEditService.RecordEditResult result = editService.deleteRecord(table, id);
            Map<String, Object> data = formatEditResponse(result);
            return McpResponse.success(data, requestId).toMap();
        } catch (Exception e) {
            Logger.log("Error removing data: " + e.getMessage());
            return McpResponse.error(500, "Error removing data: " + e.getMessage(), requestId).toMap();
        }
    }

    /**
     * 检查外键引用
     */
    private List<ForeignKeyReference> checkForeignKeyReferences(String tableName, String recordId) {
        List<ForeignKeyReference> references = new ArrayList<>();

        VTable targetTable = cfgValue.vTableMap().get(tableName);
        if (targetTable == null) {
            return references;
        }

        // 在所有表中查找引用目标记录的记录
        for (Map.Entry<String, VTable> entry : cfgValue.vTableMap().entrySet()) {
            String currentTableName = entry.getKey();
            VTable currentTable = entry.getValue();

            // 跳过目标表本身
            if (currentTableName.equals(tableName)) {
                continue;
            }

            TableSchema currentTableSchema = (TableSchema) currentTable.schema();

            // 检查当前表的外键是否引用目标表
            for (ForeignKeySchema fk : currentTableSchema.foreignKeys()) {
                if (fk.refTable().equals(tableName)) {
                    // 检查当前表中的记录是否引用了目标记录
                    List<ForeignKeyReference> tableReferences =
                        findReferencesInTable(currentTable, fk, tableName, recordId);
                    references.addAll(tableReferences);
                }
            }
        }

        return references;
    }

    /**
     * 在表中查找引用
     */
    private List<ForeignKeyReference> findReferencesInTable(VTable table, ForeignKeySchema fk,
                                                           String targetTableName, String targetRecordId) {
        List<ForeignKeyReference> references = new ArrayList<>();

        List<String> refKeys = fk.refKey().keyNames();
        List<String> localKeys = fk.key().fields();

        for (VStruct record : table.valueList()) {
            TableSchema tableSchema = (TableSchema) record.schema();
            Value pkValue = ValueUtil.extractPrimaryKeyValue(record, tableSchema);
            String recordId = pkValue.packStr();

            // 检查记录的外键字段是否匹配目标记录
            boolean matches = true;
            for (int i = 0; i < localKeys.size(); i++) {
                String localKey = localKeys.get(i);
                String refKey = refKeys.get(i);

                // 这里需要根据实际的VStruct结构来提取字段值
                // 暂时使用简单的字符串匹配作为占位实现
                String recordStr = record.packStr();
                if (!recordStr.contains(targetRecordId)) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                String title = RecordService.getBriefTitle(record);
                references.add(new ForeignKeyReference(tableSchema.name(), recordId, title, fk.name()));
            }
        }

        return references;
    }

    /**
     * 格式化外键引用信息
     */
    private List<Map<String, Object>> formatForeignKeyReferences(List<ForeignKeyReference> references) {
        List<Map<String, Object>> formatted = new ArrayList<>();
        for (ForeignKeyReference ref : references) {
            formatted.add(Map.of(
                "table", ref.getTableName(),
                "id", ref.getRecordId(),
                "title", ref.getRecordTitle(),
                "foreignKey", ref.getForeignKeyName()
            ));
        }
        return formatted;
    }

    /**
     * 格式化编辑操作响应
     */
    private Map<String, Object> formatEditResponse(RecordEditService.RecordEditResult result,
                                                  Map<String, RecordEditService.RecordEditResult> cascadeResults) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resultCode", result.resultCode().toString());
        response.put("table", result.table());
        response.put("id", result.id());

        if (!result.valueErrs().isEmpty()) {
            response.put("errors", result.valueErrs());
        }

        List<Map<String, Object>> recordIds = new ArrayList<>();
        for (SchemaService.RecordId recordId : result.recordIds()) {
            recordIds.add(Map.of(
                "id", recordId.id(),
                "title", recordId.title()
            ));
        }
        response.put("recordIds", recordIds);

        // 添加级联操作结果
        if (cascadeResults != null && !cascadeResults.isEmpty()) {
            Map<String, Object> cascadeResponse = new HashMap<>();
            for (Map.Entry<String, RecordEditService.RecordEditResult> entry : cascadeResults.entrySet()) {
                String cascadeTable = entry.getKey();
                RecordEditService.RecordEditResult cascadeResult = entry.getValue();

                cascadeResponse.put(cascadeTable, Map.of(
                    "resultCode", cascadeResult.resultCode().toString(),
                    "id", cascadeResult.id(),
                    "errors", cascadeResult.valueErrs()
                ));
            }
            response.put("cascadeResults", cascadeResponse);
        }

        return response;
    }

    /**
     * 格式化编辑操作响应（重载方法，用于没有级联操作的情况）
     */
    private Map<String, Object> formatEditResponse(RecordEditService.RecordEditResult result) {
        return formatEditResponse(result, null);
    }

    /**
     * 外键引用类
     */
    private static class ForeignKeyReference {
        private final String tableName;
        private final String recordId;
        private final String recordTitle;
        private final String foreignKeyName;

        public ForeignKeyReference(String tableName, String recordId, String recordTitle, String foreignKeyName) {
            this.tableName = tableName;
            this.recordId = recordId;
            this.recordTitle = recordTitle;
            this.foreignKeyName = foreignKeyName;
        }

        public String getTableName() { return tableName; }
        public String getRecordId() { return recordId; }
        public String getRecordTitle() { return recordTitle; }
        public String getForeignKeyName() { return foreignKeyName; }
    }
}