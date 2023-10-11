package configgen.value;

import java.util.Map;

import static configgen.value.CfgValue.*;

public class ForeachVStruct {
    private final CfgValue cfgValue;

    public ForeachVStruct(CfgValue cfgValue) {
        this.cfgValue = cfgValue;
    }

    public interface VStructVisitor {
        void accept(VStruct vStruct, VTable fromVTable);
    }

    public void forEach(VStructVisitor visitor) {
        for (VTable table : cfgValue.tables()) {
            forEachVTable(visitor, table);
        }
    }

    public void forEachVTable(VStructVisitor visitor, VTable table) {
        for (VStruct vStruct : table.valueList()) {
            forEachVStruct(visitor, vStruct, table);
        }
    }

    public void forEachVStruct(VStructVisitor visitor, VStruct vStruct, VTable table) {
        visitor.accept(vStruct, table);

        for (Value fieldValue : vStruct.values()) {
            switch (fieldValue) {
                case SimpleValue simpleValue -> {
                    forEachVStructSimpleValue(visitor, simpleValue, table);
                }

                case VList vList -> {
                    for (SimpleValue sv : vList.valueList()) {
                        forEachVStructSimpleValue(visitor, sv, table);
                    }
                }
                case VMap vMap -> {
                    for (Map.Entry<SimpleValue, SimpleValue> e : vMap.valueMap().entrySet()) {
                        forEachVStructSimpleValue(visitor, e.getKey(), table);
                        forEachVStructSimpleValue(visitor, e.getValue(), table);
                    }
                }
            }
        }
    }

    public void forEachVStructSimpleValue(VStructVisitor visitor, SimpleValue simpleValue, VTable table) {
        switch (simpleValue) {
            case PrimitiveValue _ -> {
            }
            case VInterface vInterface -> {
                forEachVStruct(visitor, vInterface.child(), table);
            }
            case VStruct vStruct -> {
                forEachVStruct(visitor, vStruct, table);
            }
        }
    }
}
