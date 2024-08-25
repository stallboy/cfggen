package configgen.value;

import configgen.data.CfgData;
import configgen.data.Source;
import configgen.schema.InterfaceSchema;
import configgen.schema.Structural;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;

public class Values {
    static CfgData.DCell ofCell(String content) {
        return CfgData.DCell.of(content, "<file>");
    }

    static VInt ofInt(int v) {
        return new VInt(v, ofCell(String.valueOf(v)));
    }

    static VLong ofLong(long v) {
        return new VLong(v, ofCell(String.valueOf(v)));
    }

    static VString ofStr(String v) {
        return new VString(v, ofCell(v));
    }

    static VBool ofBool(boolean v) {
        return new VBool(v, ofCell(String.valueOf(v)));
    }

    static VFloat ofFloat(float v) {
        return new VFloat(v, ofCell(String.valueOf(v)));
    }


    public static VStruct ofStruct(Structural schema, List<Value> values) {
        return new VStruct(schema, values, ofCell(""));
    }

    public static VInterface ofInterface(InterfaceSchema schema,
                                         VStruct child,
                                         CfgData.DCell implCell) {
        List<CfgData.DCell> list = new ArrayList<>(4);
        list.add(implCell);
        for (Value value : child.values()) {
            add(list, value.source());
        }
        return new VInterface(schema, child, Source.of(list));
    }

    public static VList ofList(List<SimpleValue> valueList) {
        return ValueUtil.createList(valueList);
    }

    public static VMap ofMap(Map<SimpleValue, SimpleValue> valueMap) {
        if (valueMap.isEmpty()) {
            return new VMap(valueMap, Source.of());
        }
        SimpleValue first = valueMap.keySet().stream().findFirst().get();
        if (first.source() instanceof Source.DFile) {
            return new VMap(valueMap, first.source());
        }

        List<CfgData.DCell> list = new ArrayList<>(valueMap.size() * 2);
        for (Map.Entry<SimpleValue, SimpleValue> v : valueMap.entrySet()) {
            add(list, v.getKey().source());
            add(list, v.getValue().source());
        }
        return new VMap(valueMap, Source.of(list));
    }

    private static void add(List<CfgData.DCell> list, Source source) {
        switch (source) {
            case CfgData.DCell dCell -> {
                list.add(dCell);
            }
            case Source.DCellList dCellList -> {
                list.addAll(dCellList.cells());
            }
            case Source.DFile ignored -> {
            }
        }

    }
}
