package configgen.value;

import configgen.schema.FieldSchema;
import configgen.util.json.JsonArr;
import configgen.util.json.JsonMap;
import configgen.util.json.JsonWriter;
import configgen.value.ValueRefCollector.FieldRef;

import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;
import static configgen.value.ValueRefCollector.RefId;
import static configgen.value.ValueRefCollector.collectStructRef;

public class ValueToJson {
    private final CfgValue cfgValue;
    private final Map<RefId, VStruct> refIdToRecordMap;
    private boolean isSaveDefault;

    public static String toJsonStr(VStruct record) {
        ValueToJson toJson = new ValueToJson();
        toJson.setSaveDefault(false);
        JsonMap jsonObject = toJson.toJson(record);
        return JsonWriter.toPrettyString(jsonObject);
    }

    public ValueToJson() {
        this(null, null);
    }

    public ValueToJson(CfgValue cfgValue, Map<RefId, VStruct> refIdToRecordMap) {
        this.cfgValue = cfgValue;
        this.refIdToRecordMap = refIdToRecordMap;
        this.isSaveDefault = true;
    }

    public void setSaveDefault(boolean saveDefault) {
        isSaveDefault = saveDefault;
    }

    public Object toJson(Value value) {
        return switch (value) {
            case VBool vBool -> vBool.value();
            case VInt vInt -> vInt.value();
            case VLong vLong -> vLong.value();
            case VFloat vFloat -> vFloat.value();
            case VString vStr -> vStr.value();
            case VText vText -> vText.original(); // 用original，因为我们用这个类ValueToJson主要是为了编辑，当保存时需要用原始文本
            case VStruct vStruct -> toJson(vStruct);
            case VInterface vInterface -> toJson(vInterface);
            case VList vList -> toJson(vList);
            case VMap vMap -> toJson(vMap);
        };
    }

    public JsonMap toJson(VStruct vStruct) {
        int count = vStruct.values().size();
        JsonMap json = new JsonMap(count + 3);

        json.put("$type", vStruct.schema().fullName());
        String note = vStruct.note();
        if (note != null && !note.isEmpty()) {
            json.put("$note", note);
        }
        if (vStruct.isFold()) {
            json.put("$fold", true);
        }

        for (int i = 0; i < count; i++) {
            FieldSchema fs = vStruct.schema().fields().get(i);
            Value fv = vStruct.values().get(i);
            if (isSaveDefault || !ValueDefault.isDefault(fv)) {
                json.put(fs.name(), toJson(fv));
            }
        }

        if (refIdToRecordMap != null) {
            List<FieldRef> fieldRefs = collectStructRef(cfgValue, vStruct, refIdToRecordMap, null, "");
            if (!fieldRefs.isEmpty()) {
                json.put("$refs", fieldRefs);
            }
        }

        return json;
    }

    public JsonMap toJson(VInterface vInterface) {
        VStruct child = vInterface.child();
        return toJson(child);
    }

    public JsonArr toJson(VList vList) {
        JsonArr json = new JsonArr(vList.valueList().size());
        for (SimpleValue sv : vList.valueList()) {
            json.add(toJson(sv));
        }
        return json;
    }

    public JsonArr toJson(VMap vMap) {
        JsonArr json = new JsonArr(vMap.valueMap().size());
        for (Map.Entry<SimpleValue, SimpleValue> e : vMap.valueMap().entrySet()) {
            SimpleValue key = e.getKey();
            SimpleValue value = e.getValue();

            JsonMap entryJson = new JsonMap(2);
            entryJson.put("$type", "$entry");
            entryJson.put("key", toJson(key));
            entryJson.put("value", toJson(value));
            json.add(entryJson);
        }
        return json;
    }
}
