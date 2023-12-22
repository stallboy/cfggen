package configgen.value;

import configgen.schema.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static configgen.value.CfgValue.*;
import static configgen.value.ValueRefCollector.*;

public class ValueRefInCollector {
    private final TableSchemaRefGraph graph;
    private final CfgValue cfgValue;

    public ValueRefInCollector(TableSchemaRefGraph graph, CfgValue cfgValue) {
        this.graph = graph;
        this.cfgValue = cfgValue;
    }


    public Map<RefId, VStruct> collect(VTable vTable, Value pkValue) {
        VStruct recordValue = vTable.primaryKeyMap().get(pkValue);
        if (recordValue == null) {
            return Map.of();
        }

        Set<String> refInTables = graph.getRefInTables(vTable.name());
        if (refInTables == null) {
            return Map.of();
        }

        if (refInTables.isEmpty()) {
            return Map.of();
        }

        Map<RefId, VStruct> result = new HashMap<>();
        for (String refInTable : refInTables) {
            VTable vRefInTable = cfgValue.vTableMap().get(refInTable);
            ForeachVStruct.VStructVisitor vStructVisitor = (vStruct, ctx) -> search(vStruct, ctx, vTable, pkValue, result);
            ForeachVStruct.foreachVTable(vStructVisitor, vRefInTable);
        }

        return result;
    }

    private static void search(VStruct vStruct, ForeachVStruct.Context ctx,
                               VTable myTable, Value myPkValue,
                               Map<RefId, VStruct> result) {

        Structural structural = vStruct.schema();
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            RefKey refKey = fk.refKey();
            if (fk.refTableSchema() != myTable.schema()) {
                continue;
            }

            // 这里只考虑primary key， TODO
            if (!(refKey instanceof RefKey.RefPrimary)) {
                continue;
            }

            FieldType ft = fk.key().fieldSchemas().getFirst().type();
            switch (ft) {
                case FieldType.SimpleType ignored -> {
                    Value localValue = ValueUtil.extractKeyValue(vStruct, fk.keyIndices());
                    if (localValue.equals(myPkValue)) {
                        addCtx(result, ctx);
                    }

                }
                case FieldType.FList ignored -> {
                    VList localList = (VList) vStruct.values().get(fk.keyIndices()[0]);
                    for (SimpleValue item : localList.valueList()) {
                        if (item.equals(myPkValue)) {
                            addCtx(result, ctx);
                        }
                    }
                }
                case FieldType.FMap ignored -> {
                    VMap localMap = (VMap) vStruct.values().get(fk.keyIndices()[0]);
                    for (SimpleValue val : localMap.valueMap().values()) {
                        if (val.equals(myPkValue)) {
                            addCtx(result, ctx);
                        }
                    }
                }
            }
        }
    }

    private static void addCtx(Map<RefId, VStruct> result, ForeachVStruct.Context ctx) {
        result.put(new RefId(ctx.fromVTable().name(), ctx.pkValue().packStr()), ctx.recordValue());
    }
}
