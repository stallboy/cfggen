package configgen.mcpserver;

import configgen.ctx.Context;
import configgen.schema.*;
import configgen.value.CfgValue;
import configgen.value.CfgValue.VStruct;
import configgen.value.CfgValue.VTable;
import configgen.util.Logger;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.util.*;

/**
 * 级联外键处理器
 * 处理数据更新时的外键级联操作
 */
public class CascadeForeignKeyProcessor {
    private final Context context;
    private final CfgValue cfgValue;

    public CascadeForeignKeyProcessor(Context context, CfgValue cfgValue) {
        this.context = context;
        this.cfgValue = cfgValue;
    }

    /**
     * 处理级联外键操作
     * @param tableName 主表名
     * @param data 主表数据
     * @return 需要级联处理的记录列表
     */
    public List<CascadeRecord> processCascadeForeignKeys(String tableName, JSONObject data) {
        List<CascadeRecord> cascadeRecords = new ArrayList<>();

        VTable vTable = cfgValue.vTableMap().get(tableName);
        if (vTable == null) {
            Logger.log("Table not found for cascade processing: " + tableName);
            return cascadeRecords;
        }

        TableSchema tableSchema = (TableSchema) vTable.schema();

        // 检查所有外键字段
        for (ForeignKeySchema fk : tableSchema.foreignKeys()) {
            String refTableName = fk.refTable();
            List<String> keys = fk.key().fields();
            List<String> refKeys = fk.refKey().keyNames();

            // 检查外键字段是否在数据中缺失
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                String refKey = refKeys.get(i);

                if (!data.containsKey(key) || data.get(key) == null) {
                    // 外键字段缺失，需要级联处理
                    CascadeRecord cascadeRecord = new CascadeRecord(refTableName, refKey, null);
                    cascadeRecords.add(cascadeRecord);
                    Logger.log("Cascade foreign key detected for table " + tableName +
                              ", missing key: " + key + ", ref table: " + refTableName);
                }
            }
        }

        return cascadeRecords;
    }

    /**
     * 创建级联记录
     * @param cascadeRecords 级联记录列表
     * @param mainTableData 主表数据（用于生成外键关联数据）
     * @return 创建的级联记录数据
     */
    public Map<String, JSONObject> createCascadeRecords(List<CascadeRecord> cascadeRecords,
                                                       JSONObject mainTableData) {
        Map<String, JSONObject> cascadeData = new HashMap<>();

        for (CascadeRecord record : cascadeRecords) {
            String refTableName = record.getRefTableName();
            String refKey = record.getRefKey();

            // 创建默认的级联记录数据
            JSONObject cascadeRecordData = createDefaultCascadeRecord(refTableName, refKey, mainTableData);

            if (cascadeRecordData != null) {
                cascadeData.put(refTableName, cascadeRecordData);
                Logger.log("Created cascade record for table " + refTableName +
                          ", key: " + refKey);
            }
        }

        return cascadeData;
    }

    /**
     * 创建默认的级联记录
     */
    private JSONObject createDefaultCascadeRecord(String refTableName, String refKey,
                                                 JSONObject mainTableData) {
        VTable refTable = cfgValue.vTableMap().get(refTableName);
        if (refTable == null) {
            Logger.log("Reference table not found: " + refTableName);
            return null;
        }

        TableSchema refTableSchema = (TableSchema) refTable.schema();
        JSONObject recordData = new JSONObject();

        // 为引用表创建默认记录
        for (FieldSchema field : refTableSchema.fields()) {
            String fieldName = field.name();

            if (fieldName.equals(refKey)) {
                // 外键字段，使用主表数据生成默认值
                Object defaultValue = generateDefaultValueForField(field);
                recordData.put(fieldName, defaultValue);
            } else {
                // 其他字段使用默认值
                Object defaultValue = generateDefaultValueForField(field);
                recordData.put(fieldName, defaultValue);
            }
        }

        return recordData;
    }

    /**
     * 为字段生成默认值
     */
    private Object generateDefaultValueForField(FieldSchema field) {
        FieldType fieldType = field.type();

        switch (fieldType) {
            case FieldType.Primitive primitive -> {
                switch (primitive) {
                    case INT -> {
                        return 0;
                    }
                    case LONG -> {
                        return 0L;
                    }
                    case FLOAT -> {
                        return 0.0f;
                    }
                    case BOOL -> {
                        return false;
                    }
                    case STRING, TEXT -> {
                        return "";
                    }
                    default -> {
                        return "";
                    }
                }
            }
            case FieldType.StructRef structRef -> {
                // 结构体引用，返回空字符串
                return "";
            }
            case FieldType.FList fList -> {
                // 列表类型，返回空列表
                return new ArrayList<>();
            }
            case FieldType.FMap fMap -> {
                // 映射类型，返回空映射
                return new HashMap<>();
            }
            default -> {
                return "";
            }
        }
    }

    /**
     * 验证级联记录是否有效
     */
    public boolean validateCascadeRecords(Map<String, JSONObject> cascadeData) {
        for (Map.Entry<String, JSONObject> entry : cascadeData.entrySet()) {
            String tableName = entry.getKey();
            JSONObject data = entry.getValue();

            VTable vTable = cfgValue.vTableMap().get(tableName);
            if (vTable == null) {
                Logger.log("Invalid cascade record: table " + tableName + " not found");
                return false;
            }

            // 这里可以添加更复杂的验证逻辑
            // 例如检查必填字段、数据类型等
        }

        return true;
    }

    /**
     * 级联记录类
     */
    public static class CascadeRecord {
        private final String refTableName;
        private final String refKey;
        private final Object defaultValue;

        public CascadeRecord(String refTableName, String refKey, Object defaultValue) {
            this.refTableName = refTableName;
            this.refKey = refKey;
            this.defaultValue = defaultValue;
        }

        public String getRefTableName() { return refTableName; }
        public String getRefKey() { return refKey; }
        public Object getDefaultValue() { return defaultValue; }
    }
}