package configgen.genlua;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static configgen.value.CfgValue.CompositeValue;


/**
 * 用于lua生成时table能共享内存就共享，以最小化客户端的内存占用
 */
class CtxShared {
    private int emptyTableUseCount = 0;
    private int listTableUseCount = 0;
    private int mapTableUseCount = 0;
    private final Map<CompositeValue, CompositeValueStr> sharedCompositeValues = new LinkedHashMap<>();


    static class CompositeValueStr {
        private String valueStr = null;
        private final String name;

        CompositeValueStr(int i) {
            name = String.format("A[%d]", i);
        }

        void setValueStr(String value) {
            valueStr = value;
        }

        String getName() {
            return name;
        }

        String getValueStr() {
            return valueStr;
        }
    }


    void parseShared(Ctx ctx) {
        ValueShared shared = new ValueShared(ctx.vTable());
        shared.iterateShared();

        // 遍历层级收集下
        int idx = 0;
        for (int i = shared.getLayers().size() - 1; i >= 0; i--) {
            ValueSharedLayer layer = shared.getLayers().get(i);
            for (ValueSharedLayer.CompositeValueCnt vc : layer.getCompositeValueToCnt().values()) {
                if (vc.getCnt() > 1) {
                    idx++;
                    AContext.getInstance().getStatistics().useSharedTable(vc.getCnt() - 1);
                    sharedCompositeValues.put(vc.getFirst(), new CompositeValueStr(idx));
                }
            }
        }

        // 生成value的字符串
        for (Map.Entry<CompositeValue, CompositeValueStr> entry : sharedCompositeValues.entrySet()) {
            StringBuilder sb = new StringBuilder();
            new ValueStringify(sb, ctx, null).addValue(entry.getKey());
            entry.getValue().setValueStr(sb.toString());
        }
    }


    Collection<CompositeValueStr> getSharedList() {
        return sharedCompositeValues.values();
    }

    String getSharedName(CompositeValue v) {
        CompositeValueStr vstr = sharedCompositeValues.get(v);
        if (vstr != null) {
            if (vstr.getValueStr() != null) {
                return vstr.getName();
            } else { //这个用于 parseShared里收集生成代码
                return null;
            }
        }
        return null;
    }


    int getEmptyTableUseCount() {
        return emptyTableUseCount;
    }

    void incEmptyTableUseCount() {
        emptyTableUseCount++;
        AContext.getInstance().getStatistics().useEmptyTable();
    }

    public boolean hasListTableOrMapTable() {
        return listTableUseCount > 0 || mapTableUseCount > 0;
    }

    void incListTableUseCount() {
        listTableUseCount++;
        AContext.getInstance().getStatistics().useListTable();
    }

    void incMapTableUseCount() {
        mapTableUseCount++;
        AContext.getInstance().getStatistics().useMapTable();
    }

}
