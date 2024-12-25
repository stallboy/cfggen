package configgen.value;

import configgen.schema.FieldSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;

public class ForeachValue {
    public interface ValueVisitor {
        void visitPrimitive(PrimitiveValue primitiveValue, Value pk, List<String> fieldChain);

        void visitVList(VList vList, Value pk, List<String> fieldChain);

        void visitVMap(VMap vMap, Value pk, List<String> fieldChain);

        void visitVInterface(VInterface vInterface, Value pk, List<String> fieldChain);

        void visitVStruct(VStruct vStruct, Value pk, List<String> fieldChain);
    }

    public static abstract class ValueVisitorForPrimitive implements ValueVisitor {

        @Override
        public void visitVList(VList vList, Value pk, List<String> fieldChain) {
        }

        @Override
        public void visitVMap(VMap vMap, Value pk, List<String> fieldChain) {
        }

        @Override
        public void visitVInterface(VInterface vInterface, Value pk, List<String> fieldChain) {
        }

        @Override
        public void visitVStruct(VStruct vStruct, Value pk, List<String> fieldChain) {
        }
    }

    public interface ValueVisitorForSearch {
        void visit(PrimitiveValue primitiveValue, String table, Value pk, List<String> fieldChain);
    }

    record ForSearchVisitor(ValueVisitorForSearch visitor,
                            String table) implements ValueVisitor {

        @Override
        public void visitPrimitive(PrimitiveValue primitiveValue, Value pk, List<String> fieldChain) {
            visitor.visit(primitiveValue, table, pk, fieldChain);
        }

        @Override
        public void visitVList(VList vList, Value pk, List<String> fieldChain) {
        }

        @Override
        public void visitVMap(VMap vMap, Value pk, List<String> fieldChain) {
        }

        @Override
        public void visitVInterface(VInterface vInterface, Value pk, List<String> fieldChain) {
            visitor.visit(new VString(vInterface.child().name(), vInterface.getImplNameSource()), table, pk, fieldChain);
            visitVStruct(vInterface.child(), pk, fieldChain);
        }

        @Override
        public void visitVStruct(VStruct vStruct, Value pk, List<String> fieldChain) {
            String note = vStruct.note();
            if (note != null && !note.isEmpty()) {
                visitor.visit(new VString(note, vStruct.source()), table, pk, subChain(fieldChain, "$note"));
            }
        }

    }

    public static void searchCfgValue(ValueVisitorForSearch visitor, CfgValue cfgValue) {
        for (VTable vTable : cfgValue.sortedTables()) {
            searchVTable(visitor, vTable);
        }
    }

    public static void searchVTable(ValueVisitorForSearch visitor, VTable vTable) {
        foreachVTable(new ForSearchVisitor(visitor, vTable.name()), vTable);
    }

    public static void foreachVTable(ValueVisitor visitor, VTable vTable) {
        for (Map.Entry<Value, VStruct> e : vTable.primaryKeyMap().entrySet()) {
            Value pk = e.getKey();
            VStruct vStruct = e.getValue();
            foreachValue(visitor, vStruct, pk, List.of());
        }
    }

    public static void foreachValue(ValueVisitor visitor, Value value,
                                    Value pk, List<String> fieldChain) {
        switch (value) {
            case PrimitiveValue primitiveValue -> {
                visitor.visitPrimitive(primitiveValue, pk, fieldChain);
            }
            case VStruct vStruct -> {
                visitor.visitVStruct(vStruct, pk, fieldChain);
                int i = 0;
                for (FieldSchema field : vStruct.schema().fields()) {
                    Value fv = vStruct.values().get(i);
                    foreachValue(visitor, fv, pk, subChain(fieldChain, field.name()));
                    i++;
                }
            }
            case VInterface vInterface -> {
                visitor.visitVInterface(vInterface, pk, fieldChain);
                int i = 0;
                for (FieldSchema field : vInterface.child().schema().fields()) {
                    Value fv = vInterface.child().values().get(i);
                    foreachValue(visitor, fv, pk, subChain(fieldChain, field.name()));
                    i++;
                }
            }
            case VList vList -> {
                visitor.visitVList(vList, pk, fieldChain);
                int i = 0;
                for (SimpleValue sv : vList.valueList()) {
                    foreachValue(visitor, sv, pk, subChain(fieldChain, String.valueOf(i)));
                    i++;
                }
            }
            case VMap vMap -> {
                visitor.visitVMap(vMap, pk, fieldChain);
                int i = 0;
                for (Map.Entry<SimpleValue, SimpleValue> entry : vMap.valueMap().entrySet()) {
                    foreachValue(visitor, entry.getKey(), pk, subChain(fieldChain, String.format("%dk", i)));
                    foreachValue(visitor, entry.getValue(), pk, subChain(fieldChain, String.format("%dv", i)));
                    i++;
                }
            }
        }
    }

    private static List<String> subChain(List<String> old, String e) {
        List<String> res = new ArrayList<>(old.size() + 1);
        res.addAll(old);
        res.add(e);
        return res;
    }

}
