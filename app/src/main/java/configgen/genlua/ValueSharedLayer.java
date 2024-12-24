package configgen.genlua;

import configgen.value.ValueHasText;

import java.util.LinkedHashMap;
import java.util.Map;

import static configgen.value.CfgValue.*;

class ValueSharedLayer {
    static class CompositeValueCnt {
        private int cnt;
        private final CompositeValue first;
        private boolean traversed = false;

        CompositeValueCnt(CompositeValue first) {
            this.first = first;
            cnt = 1;
        }

        int getCnt() {
            return cnt;
        }

        void incCnt() {
            cnt++;
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

    public ValueSharedLayer(ValueShared shared) {
        this.shared = shared;
        compositeValueToCnt = new LinkedHashMap<>();
    }


    public Map<CompositeValue, CompositeValueCnt> getCompositeValueToCnt() {
        return compositeValueToCnt;
    }

    private void add(CompositeValue v) {
        CompositeValueCnt oldInThisLayer = compositeValueToCnt.get(v);
        boolean isLangSwitchAndHasText = isLangSwitchAndCompositeValueHasText(v);
        if (oldInThisLayer != null) {
            // 1，本层中包含了
            if (!isLangSwitchAndHasText){ // 此时是不共享的，因为虽然VText的原始文本一样，但翻译可能不同
                oldInThisLayer.incCnt();
                oldInThisLayer.first.setShared(); //设置上，后面生成代码时会快点
                v.setShared();
            } // else 忽略
        } else {
            CompositeValueCnt oldInPreviousLayer = shared.remove(v); // 这个会从之前的layer中删除
            if (oldInPreviousLayer != null) {
                // 2，本层未包含，之前的层包含
                if (!isLangSwitchAndHasText){
                    oldInPreviousLayer.incCnt();
                    // 挪到这层，这样生成lua代码时已经排序,但要在生成下层shared时不遍历这个，因为已经遍历过
                    compositeValueToCnt.put(v, oldInPreviousLayer);

                    oldInPreviousLayer.first.setShared();
                    v.setShared();
                } // else 忽略

            } else {
                // 3，本层未包含，之前层也未包含
                compositeValueToCnt.put(v, new CompositeValueCnt(v));
            }
        }
    }

    public void visitSubStructs(Value value) {
        switch (value) {
            case PrimitiveValue ignored -> {
            }
            case VStruct vStruct -> {
                for (Value fv : vStruct.values()) {
                    visitThis(fv);
                }
            }
            case VInterface vInterface -> {
                for (Value fv : vInterface.child().values()) {
                    visitThis(fv);
                }
            }
            case VList vList -> {
                for (SimpleValue item : vList.valueList()) {
                    visitThis(item);
                }

            }
            case VMap vMap -> {
                for (Map.Entry<SimpleValue, SimpleValue> entry : vMap.valueMap().entrySet()) {
                    visitThis(entry.getKey());
                    visitThis(entry.getValue());
                }
            }
        }
    }

    private void visitThis(Value value) {
        switch (value) {
            case PrimitiveValue ignored -> {
            }
            case VStruct vStruct -> {
                if (!vStruct.values().isEmpty()) {
                    add(vStruct);
                }
            }
            case VInterface vInterface -> {
                add(vInterface);
            }
            case VList vList -> {
                if (!vList.valueList().isEmpty()) {
                    add(vList);
                }
            }
            case VMap vMap -> {
                if (!vMap.valueMap().isEmpty()) {
                    add(vMap);
                }
            }
        }
    }

    private static boolean isLangSwitchAndCompositeValueHasText(CompositeValue value) {
        return AContext.getInstance().nullableLangSwitchSupport() != null &&
                ValueHasText.hasText(value);
    }

}
