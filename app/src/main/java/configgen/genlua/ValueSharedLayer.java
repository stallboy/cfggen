package configgen.genlua;

import java.util.LinkedHashMap;
import java.util.Map;

import static configgen.value.CfgValue.*;

class ValueSharedLayer {
    static class CompositeValueCnt {
        int cnt;
        CompositeValue first;
        private boolean traversed = false;

        CompositeValueCnt(CompositeValue first) {
            this.first = first;
            cnt = 1;
        }

        int getCnt() {
            return cnt;
        }

        CompositeValue getFirst() {
            return first;
        }

        boolean isTraversed() {
            return traversed;
        }

        void setTraversed() {
            this.traversed = true;
        }
    }

    private final ValueShared shared;
    private final Map<CompositeValue, CompositeValueCnt> compositeValueToCnt;
    private final ValueSharedLayer next;

    ValueSharedLayer(ValueShared shared) {
        this.shared = shared;
        compositeValueToCnt = new LinkedHashMap<>();
        next = new ValueSharedLayer(this);
    }

    private ValueSharedLayer(ValueSharedLayer last) {
        shared = last.shared;
        compositeValueToCnt = last.compositeValueToCnt;
        next = null;
    }

    Map<CompositeValue, CompositeValueCnt> getCompositeValueToCnt() {
        return compositeValueToCnt;
    }

    private void add(CompositeValue v) {
        CompositeValueCnt oldInThisLayer = compositeValueToCnt.get(v);
        if (oldInThisLayer != null) {
            oldInThisLayer.cnt++;
            oldInThisLayer.first.setShared(); //设置上，后面生成代码时会快点
            v.setShared();
        } else {
            CompositeValueCnt oldInPreviousLayer = shared.remove(v); //这个会从之前的layer中删除
            if (oldInPreviousLayer != null) { //前面的层可能包含了这个v
                oldInPreviousLayer.cnt++;
                //挪到这层，这样生成lua代码时已经排序,但要在生成下层shared时不遍历这个，因为已经遍历过
                compositeValueToCnt.put(v, oldInPreviousLayer);

                oldInPreviousLayer.first.setShared();
                v.setShared();

            } else {
                compositeValueToCnt.put(v, new CompositeValueCnt(v));
            }
        }
    }

    void visit(Value value) {
        switch (value) {
            case PrimitiveValue _ -> {
            }
            case VStruct vStruct -> visitVStruct(vStruct);
            case VInterface vInterface -> visitVInterface(vInterface);
            case VList vList -> visitVList(vList);
            case VMap vMap -> visitVMap(vMap);
        }
    }

    void visitVList(VList value) {
        if (next != null) {
            for (SimpleValue item : value.valueList()) {
                next.visit(item);
            }
        } else if (!value.valueList().isEmpty()) {
            add(value);
        }
    }

    void visitVMap(VMap value) {
        if (next != null) {
            for (Map.Entry<SimpleValue, SimpleValue> entry : value.valueMap().entrySet()) {
                next.visit(entry.getKey());
                next.visit(entry.getValue());
            }
        } else if (!value.valueMap().isEmpty()) {
            add(value);
        }
    }

    void visitVStruct(VStruct value) {
        if (next != null) {
            for (Value field : value.values()) {
                next.visit(field);
            }
        } else if (!value.values().isEmpty()) {
            add(value);
        }
    }

    void visitVInterface(VInterface value) {
        if (next != null) {
            for (Value field : value.child().values()) {
                next.visit(field);
            }
        } else {
            add(value);
        }
    }
}
