package configgen.value;

import configgen.data.CfgData;
import configgen.schema.InterfaceSchema;
import configgen.schema.Structural;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;

public class Values {
    static CfgData.DCell ofCell(String str) {
        return new CfgData.DCell(str, new CfgData.DRowId("fileName", "sheetName", 0), 0, (byte) 0);
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
        return new VInterface(schema, child, implCell);
    }
    public static VList ofList(List<SimpleValue> valueList) {

        return new VList(valueList, ofCell(""));
    }

    public static VMap of(Map<SimpleValue, SimpleValue> valueMap) {
//        List<CfgData.DCell> cells = new ArrayList<>(8);
//        for (var e : valueMap.entrySet()) {
//            cells.addAll(e.getKey().cells());
//            cells.addAll(e.getValue().cells());
//        }
        return new VMap(valueMap, ofCell(""));
    }
}
