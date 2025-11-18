package configgen.mcpserver.services;

import configgen.ctx.Context;
import configgen.schema.*;
import configgen.schema.cfg.CfgWriter;
import configgen.util.Logger;

import java.util.*;
import static configgen.schema.FieldType.*;

/**
 * 模式服务
 */
public class SchemaService {
    private final Context context;

    public SchemaService(Context context) {
        this.context = context;
    }

    /**
     * 构建模式文本
     * @param mainTable 主表
     * @return 模式文本
     */
    public String buildSchemaText(TableSchema mainTable) {
        Set<String> visited = new LinkedHashSet<>();
        List<Nameable> ordered = new ArrayList<>();

        visited.add(mainTable.name());
        ordered.add(mainTable);

        TableSchemaRefGraph graph = new TableSchemaRefGraph(context.cfgSchema());
        TableSchemaRefGraph.Refs refs = graph.refsMap().get(mainTable.name());
        if (refs != null) {
            for (TableSchema t : refs.refOutTables().values()) {
                if (visited.add(t.name())) {
                    ordered.add(t);
                }
            }
            for (TableSchema t : refs.refInTables().values()) {
                if (visited.add(t.name())) {
                    ordered.add(t);
                }
            }
        }

        // 收集所有相关的结构体定义
        Set<StructSchema> relatedStructs = new LinkedHashSet<>();
        for (Nameable item : ordered) {
            if (item instanceof TableSchema table) {
                collectRelatedStructs(table, relatedStructs);
            }
        }

        Logger.log("Collected structs: " + relatedStructs.size());
        for (StructSchema struct : relatedStructs) {
            Logger.log("Struct: " + struct.name());
        }

        CfgSchema sub = CfgSchema.ofPartial();
        // 先添加结构体定义
        for (StructSchema struct : relatedStructs) {
            sub.add(struct);
        }
        // 再添加表定义
        for (Nameable item : ordered) {
            sub.add(item);
        }
        return CfgWriter.stringify(sub, true, false);
    }

    /**
     * 收集表相关的所有结构体定义
     */
    private void collectRelatedStructs(TableSchema table, Set<StructSchema> structs) {
        // 收集表字段中使用的结构体类型
        for (FieldSchema field : table.fields()) {
            collectStructsFromFieldType(field.type(), structs);
        }

        // 收集外键引用中使用的结构体
        for (ForeignKeySchema fk : table.foreignKeys()) {
            // 外键本身不直接包含结构体，但引用的表可能包含
            TableSchema refTable = fk.refTableSchema();
            if (refTable != null) {
                collectRelatedStructs(refTable, structs);
            }
        }
    }

    /**
     * 从字段类型中收集结构体定义
     */
    private void collectStructsFromFieldType(FieldType fieldType, Set<StructSchema> structs) {
        Logger.log("collectStructsFromFieldType: " + fieldType.getClass().getSimpleName());
        switch (fieldType) {
            case StructRef structRef -> {
                Logger.log("Found StructRef: " + structRef.name());

                // 使用fieldableMap直接获取结构体定义，避免依赖StructRef.obj()
                String structName = structRef.name();
                Fieldable fieldable = context.cfgSchema().fieldableMap().get(structName);
                Logger.log("Fieldable from map: " + (fieldable != null ? fieldable.getClass().getSimpleName() : "null"));

                if (fieldable instanceof StructSchema structSchema) {
                    Logger.log("Adding struct from fieldableMap: " + structSchema.name());
                    structs.add(structSchema);
                    // 递归收集该结构体的字段中的结构体
                    for (FieldSchema field : structSchema.fields()) {
                        collectStructsFromFieldType(field.type(), structs);
                    }
                } else {
                    // 如果fieldableMap中没有找到，尝试使用原始的StructRef.obj()作为后备
                    Nameable ref = structRef.obj();
                    Logger.log("Ref obj type: " + (ref != null ? ref.getClass().getSimpleName() : "null"));
                    if (ref instanceof StructSchema structSchema) {
                        Logger.log("Adding struct from StructRef.obj(): " + structSchema.name());
                        structs.add(structSchema);
                        // 递归收集该结构体的字段中的结构体
                        for (FieldSchema field : structSchema.fields()) {
                            collectStructsFromFieldType(field.type(), structs);
                        }
                    } else {
                        Logger.log("WARNING: Could not find struct definition for: " + structName);
                    }
                }
            }
            case FList fList -> {
                Logger.log("Found FList");
                collectStructsFromFieldType(fList.item(), structs);
            }
            case FMap fMap -> {
                Logger.log("Found FMap");
                collectStructsFromFieldType(fMap.key(), structs);
                collectStructsFromFieldType(fMap.value(), structs);
            }
            default -> {
                Logger.log("Basic type: " + fieldType);
                // 基本类型，不需要处理
            }
        }
    }

    /**
     * 调试schema状态，了解结构体解析情况
     */
    public void debugSchemaState() {
        CfgSchema schema = context.cfgSchema();
        Logger.log("Fieldable map size: " + schema.fieldableMap().size());

        // Log all available structs
        int structCount = 0;
        for (Map.Entry<String, Fieldable> entry : schema.fieldableMap().entrySet()) {
            if (entry.getValue() instanceof StructSchema) {
                Logger.log("Available struct: " + entry.getKey());
                structCount++;
            }
        }
        Logger.log("Total structs in schema: " + structCount);

        // Log all tables
        Logger.log("Total tables in schema: " + schema.tableMap().size());
        for (String tableName : schema.tableMap().keySet()) {
            Logger.log("Available table: " + tableName);
        }
    }
}