package configgen.value;

import configgen.schema.FieldSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;

public class ForeachPrimitiveValue {

    public interface PrimitiveValueVisitor {
        void visit(PrimitiveValue primitiveValue, String table, Value primaryKey, List<String> fieldChain);
    }

    public static void foreach(PrimitiveValueVisitor visitor, CfgValue cfgValue) {
        for (VTable vTable : cfgValue.sortedTables()) {
            foreachVTable(visitor, vTable);
        }
    }

    public static void foreachVTable(PrimitiveValueVisitor visitor, VTable vTable) {
        for (Map.Entry<Value, VStruct> e : vTable.primaryKeyMap().entrySet()) {
            Value pk = e.getKey();
            VStruct vStruct = e.getValue();
            foreachValue(visitor, vStruct, vTable.name(), pk, List.of());
        }
    }

    public static void foreachValue(PrimitiveValueVisitor visitor, Value value, String table, Value pk, List<String> fieldChain) {
        switch (value) {
            case PrimitiveValue primitiveValue -> {
                visitor.visit(primitiveValue, table, pk, fieldChain);
            }
            case VStruct vStruct -> {
                int i = 0;
                for (FieldSchema field : vStruct.schema().fields()) {
                    Value fv = vStruct.values().get(i);
                    foreachValue(visitor, fv, table, pk, listAddOf(fieldChain, field.name()));
                    i++;
                }
            }
            case VInterface vInterface -> {
                visitor.visit(new VString(vInterface.child().name(), vInterface.getImplNameSource()), table, pk, fieldChain);
                foreachValue(visitor, vInterface.child(), table, pk, fieldChain);
            }
            case VList vList -> {
                int i = 0;
                for (SimpleValue sv : vList.valueList()) {
                    foreachValue(visitor, sv, table, pk, listAddOf(fieldChain, String.valueOf(i)));
                    i++;
                }
            }
            case VMap vMap -> {
                int i = 0;
                for (Map.Entry<SimpleValue, SimpleValue> entry : vMap.valueMap().entrySet()) {
                    foreachValue(visitor, entry.getKey(), table, pk, listAddOf(fieldChain, String.format("%dk", i)));
                    foreachValue(visitor, entry.getValue(), table, pk, listAddOf(fieldChain, String.format("%dv", i)));
                    i++;
                }
            }
        }
    }

    private static List<String> listAddOf(List<String> old, String e) {
        List<String> res = new ArrayList<>(old.size() + 1);
        res.addAll(old);
        res.add(e);
        return res;
    }

}
