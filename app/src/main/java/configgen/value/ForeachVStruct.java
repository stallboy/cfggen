package configgen.value;

import java.util.Map;

import static configgen.value.CfgValue.*;

public class ForeachVStruct {

    public interface VStructVisitor {
        void visit(VStruct vStruct, VTable fromVTable);
    }

    public static void foreach(VStructVisitor visitor, CfgValue cfgValue) {
        for (VTable table : cfgValue.sortedTables()) {
            foreachVTable(visitor, table);
        }
    }

    public static void foreachVTable(VStructVisitor visitor, VTable table) {
        for (VStruct vStruct : table.valueList()) {
            foreachVStruct(visitor, vStruct, table);
        }
    }

    public static void foreachVStruct(VStructVisitor visitor, VStruct vStruct, VTable table) {
        visitor.visit(vStruct, table);

        for (Value fieldValue : vStruct.values()) {
            switch (fieldValue) {
                case SimpleValue simpleValue -> {
                    foreachVStructSimpleValue(visitor, simpleValue, table);
                }

                case VList vList -> {
                    for (SimpleValue sv : vList.valueList()) {
                        foreachVStructSimpleValue(visitor, sv, table);
                    }
                }
                case VMap vMap -> {
                    for (Map.Entry<SimpleValue, SimpleValue> e : vMap.valueMap().entrySet()) {
                        foreachVStructSimpleValue(visitor, e.getKey(), table);
                        foreachVStructSimpleValue(visitor, e.getValue(), table);
                    }
                }
            }
        }
    }

    public static void foreachVStructSimpleValue(VStructVisitor visitor, SimpleValue simpleValue, VTable table) {
        switch (simpleValue) {
            case PrimitiveValue _ -> {
            }
            case VInterface vInterface -> foreachVStruct(visitor, vInterface.child(), table);
            case VStruct vStruct -> foreachVStruct(visitor, vStruct, table);
        }
    }
}
