package configgen.value;

import java.util.Map;

import static configgen.value.CfgValue.*;

public class ForeachVStruct {

    public record Context(VTable fromVTable,
                          Value pkValue,
                          VStruct recordValue) {
        String recordId() {
            return String.format("%s-%s", fromVTable.name(), pkValue.packStr());
        }
    }

    public interface VStructVisitor {
        /**
         * 访问一个 VStruct
         * @return true 表示继续遍历，false 表示停止遍历
         */
        boolean visit(VStruct vStruct, Context ctx);
    }

    public static void foreach(VStructVisitor visitor, CfgValue cfgValue) {
        for (VTable table : cfgValue.sortedTables()) {
            boolean shouldContinue = foreachVTable(visitor, table);
            if (!shouldContinue) {
                break;  // 提前终止
            }
        }
    }

    public static boolean foreachVTable(VStructVisitor visitor, VTable table) {
        for (Map.Entry<Value, VStruct> e : table.primaryKeyMap().entrySet()) {
            boolean shouldContinue = foreachVStruct(visitor, e.getValue(),
                    new Context(table, e.getKey(), e.getValue()));
            if (!shouldContinue) {
                return false;  // 提前终止
            }
        }
        return true;  // 继续遍历
    }

    private static boolean foreachVStruct(VStructVisitor visitor, VStruct vStruct, Context ctx) {
        boolean shouldContinue = visitor.visit(vStruct, ctx);
        if (!shouldContinue) {
            return false;  // visitor 要求停止
        }

        for (Value fieldValue : vStruct.values()) {
            switch (fieldValue) {
                case SimpleValue simpleValue -> {
                    boolean continueValue = foreachVStructSimpleValue(visitor, simpleValue, ctx);
                    if (!continueValue) {
                        return false;
                    }
                }

                case VList vList -> {
                    for (SimpleValue sv : vList.valueList()) {
                        boolean continueList = foreachVStructSimpleValue(visitor, sv, ctx);
                        if (!continueList) {
                            return false;
                        }
                    }
                }
                case VMap vMap -> {
                    for (Map.Entry<SimpleValue, SimpleValue> e : vMap.valueMap().entrySet()) {
                        boolean continueKey = foreachVStructSimpleValue(visitor, e.getKey(), ctx);
                        if (!continueKey) {
                            return false;
                        }
                        boolean continueVal = foreachVStructSimpleValue(visitor, e.getValue(), ctx);
                        if (!continueVal) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private static boolean foreachVStructSimpleValue(VStructVisitor visitor, SimpleValue simpleValue, Context ctx) {
        switch (simpleValue) {
            case PrimitiveValue ignored -> {
                return true;  // 继续遍历
            }
            case VInterface vInterface -> {
                return foreachVStruct(visitor, vInterface.child(), ctx);
            }
            case VStruct vStruct -> {
                return foreachVStruct(visitor, vStruct, ctx);
            }
        }
    }
}
