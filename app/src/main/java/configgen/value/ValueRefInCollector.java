package configgen.value;

import configgen.schema.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static configgen.value.CfgValue.*;
import static configgen.value.ValueRefCollector.*;
import configgen.schema.FindFieldIndex;

public class ValueRefInCollector {
    private final TableSchemaRefGraph graph;
    private final CfgValue cfgValue;

    public ValueRefInCollector(TableSchemaRefGraph graph, CfgValue cfgValue) {
        this.graph = graph;
        this.cfgValue = cfgValue;
    }

    public Map<RefId, ForeachVStruct.Context> collect(VTable vTable, Value pkValue) {
        Map<RefId, ForeachVStruct.Context> result = new LinkedHashMap<>();
        collectTo(vTable, pkValue, result);
        return result;
    }

    /**
     * 快速检查给定的记录是否被任何表引用。
     * 在找到第一个引用时立即返回，避免构建完整的引用信息。
     *
     * @param vTable 要检查的表
     * @param pkValue 主键值
     * @return 如果存在至少一个引用返回 true，否则返回 false
     */
    public boolean hasReference(VTable vTable, Value pkValue) {
        SearchParams params = buildSearchParams(vTable, pkValue);
        if (params == null) {
            return false;
        }

        return searchReferences(params.refInTables(), vTable, pkValue, null, true);
    }

    public void collectTo(VTable vTable, Value pkValue, Map<RefId, ForeachVStruct.Context> result) {
        SearchParams params = buildSearchParams(vTable, pkValue);
        if (params == null) {
            return;
        }

        searchReferences(params.refInTables(), vTable, pkValue, result, false);
    }

    /**
     * 构建搜索参数，执行前置检查
     *
     * @param vTable 要检查的表
     * @param pkValue 主键值
     * @return 搜索参数，如果前置检查失败则返回 null
     */
    private SearchParams buildSearchParams(VTable vTable, Value pkValue) {
        // 检查记录是否存在
        if (vTable.primaryKeyMap().get(pkValue) == null) {
            return null;
        }

        // 检查是否有表引用到此表
        TableSchemaRefGraph.Refs refs = graph.refsMap().get(vTable.name());
        if (refs == null) {
            return null;
        }

        Set<String> refInTables = refs.refIn();
        if (refInTables.isEmpty()) {
            return null;
        }

        return new SearchParams(refInTables);
    }

    /**
     * 搜索引用表中的所有引用
     *
     * @param refInTables 引用此表的所有表名
     * @param targetTable 目标表
     * @param targetPkValue 目标主键值
     * @param result 收集结果（null 表示不收集，只检查是否存在）
     * @param stopAtFirst 是否在找到第一个引用时停止
     * @return 是否找到引用
     */
    private boolean searchReferences(Set<String> refInTables,
                                     VTable targetTable,
                                     Value targetPkValue,
                                     Map<RefId, ForeachVStruct.Context> result,
                                     boolean stopAtFirst) {
        for (String refInTable : refInTables) {
            VTable vRefInTable = cfgValue.vTableMap().get(refInTable);
            boolean found = searchInTable(vRefInTable, targetTable, targetPkValue, result, stopAtFirst);
            if (found && stopAtFirst) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在单个表中搜索引用
     *
     * @param searchTable 要搜索的表
     * @param targetTable 目标表
     * @param targetPkValue 目标主键值
     * @param result 收集结果（null 表示不收集）
     * @param stopAtFirst 是否在找到第一个引用时停止
     * @return 是否找到引用
     */
    private static boolean searchInTable(VTable searchTable,
                                         VTable targetTable,
                                         Value targetPkValue,
                                         Map<RefId, ForeachVStruct.Context> result,
                                         boolean stopAtFirst) {
        // 获取目标记录（提前获取，避免在循环中重复查找）
        VStruct targetRecord = targetTable.primaryKeyMap().get(targetPkValue);
        if (targetRecord == null) {
            return false;
        }

        boolean[] found = {false};

        ForeachVStruct.VStructVisitor visitor = (vStruct, ctx) -> {
            Structural structural = vStruct.schema();
            for (ForeignKeySchema fk : structural.foreignKeys()) {
                if (fk.refTableSchema() != targetTable.schema()) {
                    continue;
                }

                boolean matchFound = checkForeignKeyMatch(vStruct, fk, targetRecord);
                if (matchFound) {
                    if (result != null) {
                        addCtx(result, ctx);
                    }
                    found[0] = true;
                    if (stopAtFirst) {
                        return false;
                    }
                }
            }
            return true;
        };

        ForeachVStruct.foreachVTable(visitor, searchTable);
        return found[0];
    }

    /**
     * 检查外键值是否匹配目标主键值
     */
    private static boolean checkForeignKeyMatch(VStruct vStruct, ForeignKeySchema fk, VStruct targetRecord) {
        // 获取目标记录的键值（根据 refKey 类型决定是主键、唯一键还是其他键）
        Value targetValue = getTargetValue(fk, targetRecord);

        FieldType ft = fk.key().fieldSchemas().getFirst().type();

        return switch (ft) {
            case FieldType.SimpleType ignored -> {
                Value localValue = ValueUtil.extractKeyValue(vStruct, fk.keyIndices());
                yield localValue.equals(targetValue);
            }
            case FieldType.FList ignored -> {
                VList localList = (VList) vStruct.values().get(fk.keyIndices()[0]);
                yield localList.valueList().stream().anyMatch(item -> item.equals(targetValue));
            }
            case FieldType.FMap ignored -> {
                VMap localMap = (VMap) vStruct.values().get(fk.keyIndices()[0]);
                yield localMap.valueMap().values().stream().anyMatch(val -> val.equals(targetValue));
            }
        };
    }

    /**
     * 根据外键的 refKey 类型，获取目标记录的对应键值
     * 使用 ValueUtil 中已有的方法来提取值
     */
    private static Value getTargetValue(ForeignKeySchema fk, VStruct targetRecord) {
        RefKey refKey = fk.refKey();
        Structural structural = targetRecord.schema();

        if (!(structural instanceof TableSchema tableSchema)) {
            throw new IllegalArgumentException("targetRecord schema must be a TableSchema, but got: " + structural.getClass());
        }

        if (refKey instanceof RefKey.RefPrimary) {
            // RefPrimary: 使用 ValueUtil.extractPrimaryKeyValue
            return ValueUtil.extractPrimaryKeyValue(targetRecord, tableSchema);
        } else if (refKey instanceof RefKey.RefUniq refUniq) {
            // RefUniq: 使用 findFieldIndices + extractKeyValue
            KeySchema uniqKey = tableSchema.findUniqueKey(refUniq.keyNames());
            int[] indices = FindFieldIndex.findFieldIndices(tableSchema, uniqKey);
            return ValueUtil.extractKeyValue(targetRecord, indices);
        } else if (refKey instanceof RefKey.RefList refList) {
            // RefList: 使用 findFieldIndices + extractKeyValue
            // RefList 的 keyNames 指向被引用表的键（可能是主键或唯一键）
            KeySchema refKeySchema;
            if (tableSchema.primaryKey().fields().equals(refList.keyNames())) {
                refKeySchema = tableSchema.primaryKey();
            } else {
                refKeySchema = tableSchema.findUniqueKey(refList.keyNames());
                if (refKeySchema == null) {
                    throw new IllegalArgumentException("RefList key not found: " + refList.keyNames());
                }
            }
            int[] indices = FindFieldIndex.findFieldIndices(tableSchema, refKeySchema);
            return ValueUtil.extractKeyValue(targetRecord, indices);
        }

        throw new IllegalArgumentException("Unknown RefKey type: " + refKey.getClass());
    }

    private static void addCtx(Map<RefId, ForeachVStruct.Context> result, ForeachVStruct.Context ctx) {
        result.put(new RefId(ctx.fromVTable().name(), ctx.pkValue().packStr()), ctx);
    }

    /**
     * 搜索参数封装
     */
    private record SearchParams(Set<String> refInTables) {
    }
}
