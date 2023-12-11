package configgen.value;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import configgen.schema.FieldSchema;

import java.util.Map;

import static configgen.value.CfgValue.*;

public class ValueJson {

    public static Object toJson(Value value) {
        return switch (value) {
            case VBool vBool -> vBool.value();
            case VInt vInt -> vInt.value();
            case VLong vLong -> vLong.value();
            case VFloat vFloat -> vFloat.value();
            case VString vStr -> vStr.value();
            case VText vText -> vText.value();
            case VStruct vStruct -> toJson(vStruct);
            case VInterface vInterface -> toJson(vInterface);
            case VList vList -> toJson(vList);
            case VMap vMap -> toJson(vMap);
        };
    }

    public static JSONObject toJson(VStruct vStruct) {
        int count = vStruct.values().size();
        JSONObject json = new JSONObject(count);
        for (int i = 0; i < count; i++) {
            FieldSchema fs = vStruct.schema().fields().get(i);
            Value fv = vStruct.values().get(i);
            json.put(fs.name(), toJson(fv));
        }
        return json;

    }

    public static JSONObject toJson(VInterface vInterface) {
        VStruct child = vInterface.child();
        JSONObject json = toJson(child);
        json.put("$type", child.name());
        return json;
    }


    public static JSONArray toJson(VList vList) {
        JSONArray json = new JSONArray(vList.valueList().size());
        for (SimpleValue sv : vList.valueList()) {
            json.add(toJson(sv));
        }
        return json;
    }

    public static JSONArray toJson(VMap vMap) {
        JSONArray json = new JSONArray(vMap.valueMap().size());
        for (Map.Entry<SimpleValue, SimpleValue> e : vMap.valueMap().entrySet()) {
            SimpleValue key = e.getKey();
            SimpleValue value = e.getValue();
            json.add(key);
            json.add(value);
        }
        return json;
    }
}
