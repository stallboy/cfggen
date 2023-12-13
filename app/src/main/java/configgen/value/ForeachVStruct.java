package configgen.value;

import java.util.Map;

import static configgen.value.CfgValue.*;

public class ForeachVStruct {

    public record Context(VTable fromVTable,
                          Value pkValue,
                          Value recordValue) {
    }

    public interface VStructVisitor {
        void visit(VStruct vStruct, Context ctx);
    }

    public static void foreach(VStructVisitor visitor, CfgValue cfgValue) {
        for (VTable table : cfgValue.sortedTables()) {
            foreachVTable(visitor, table);
        }
    }

    public static void foreachVTable(VStructVisitor visitor, VTable table) {
        for (Map.Entry<Value, VStruct> e : table.primaryKeyMap().entrySet()) {
            foreachVStruct(visitor, e.getValue(), new Context(table, e.getKey(), e.getValue()));
        }
    }

    private static void foreachVStruct(VStructVisitor visitor, VStruct vStruct, Context ctx) {
        visitor.visit(vStruct, ctx);

        for (Value fieldValue : vStruct.values()) {
            switch (fieldValue) {
                case SimpleValue simpleValue -> {
                    foreachVStructSimpleValue(visitor, simpleValue, ctx);
                }

                case VList vList -> {
                    for (SimpleValue sv : vList.valueList()) {
                        foreachVStructSimpleValue(visitor, sv, ctx);
                    }
                }
                case VMap vMap -> {
                    for (Map.Entry<SimpleValue, SimpleValue> e : vMap.valueMap().entrySet()) {
                        foreachVStructSimpleValue(visitor, e.getKey(), ctx);
                        foreachVStructSimpleValue(visitor, e.getValue(), ctx);
                    }
                }
            }
        }
    }

    private static void foreachVStructSimpleValue(VStructVisitor visitor, SimpleValue simpleValue, Context ctx) {
        switch (simpleValue) {
            case PrimitiveValue ignored -> {
            }
            case VInterface vInterface -> foreachVStruct(visitor, vInterface.child(), ctx);
            case VStruct vStruct -> foreachVStruct(visitor, vStruct, ctx);
        }
    }
}
