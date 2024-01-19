package configgen.value;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import configgen.schema.FieldSchema;
import configgen.value.ValueRefCollector.FieldRef;

import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;
import static configgen.value.ValueRefCollector.RefId;
import static configgen.value.ValueRefCollector.collectStructRef;

public class ValueToJson {
    private final CfgValue cfgValue;
    private final Map<RefId, VStruct> refIdToRecordMap;

    public ValueToJson() {
        this(null, null);
    }

    public ValueToJson(CfgValue cfgValue, Map<RefId, VStruct> refIdToRecordMap) {
        this.cfgValue = cfgValue;
        this.refIdToRecordMap = refIdToRecordMap;
    }

    public Object toJson(Value value) {
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

    public JSONObject toJson(VStruct vStruct) {
        int count = vStruct.values().size();
        JSONObject json = new JSONObject(count);
        for (int i = 0; i < count; i++) {
            FieldSchema fs = vStruct.schema().fields().get(i);
            Value fv = vStruct.values().get(i);
            json.put(fs.name(), toJson(fv));
        }
        json.put("$type", vStruct.schema().fullName());
        if (refIdToRecordMap != null) {
            List<FieldRef> fieldRefs = collectStructRef(cfgValue, vStruct, refIdToRecordMap, null, "");
            if (!fieldRefs.isEmpty()) {
                json.put("$refs", fieldRefs);
            }
        }

        return json;
    }

    public JSONObject toJson(VInterface vInterface) {
        VStruct child = vInterface.child();
        return toJson(child);
    }

    public JSONArray toJson(VList vList) {
        JSONArray json = new JSONArray(vList.valueList().size());
        for (SimpleValue sv : vList.valueList()) {
            json.add(toJson(sv));
        }
        return json;
    }

    public JSONArray toJson(VMap vMap) {
        JSONArray json = new JSONArray(vMap.valueMap().size());
        for (Map.Entry<SimpleValue, SimpleValue> e : vMap.valueMap().entrySet()) {
            SimpleValue key = e.getKey();
            SimpleValue value = e.getValue();

            JSONObject entryJson = new JSONObject(2);
            entryJson.put("key", toJson(key));
            entryJson.put("value", toJson(value));
            entryJson.put("$type", "$entry");

            json.add(entryJson);
        }
        return json;
    }
}
