package configgen.value;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import configgen.schema.FieldSchema;
import configgen.value.ValueRefCollector.FieldRef;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
        JSONObject jsonObject = toJson.toJson(record);
        return JSON.toJSONString(jsonObject, JSONWriter.Feature.PrettyFormat);
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

    public JSONObject toJson(VStruct vStruct) {
        int count = vStruct.values().size();
        JSONObject json = new JSONObject(count + 3);

        json.put("$type", vStruct.schema().fullName());
        String note = vStruct.note();
        if (note != null && !note.isEmpty()) {
            json.put("$note", note);
        }
        if (vStruct.isFold()) {
            json.put("$fold", true);
        }
        Map<String, Boolean> embedFields = vStruct.embedFields();
        if (embedFields != null) {
            json.putAll(embedFields); // 透传cfgeditor写入的$embed_<fieldName>
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
        Map<SimpleValue, Boolean> entryEmbeds = vMap.entryEmbeds();
        Set<SimpleValue> foldedEntries = vMap.foldedEntries();
        Map<SimpleValue, String> entryNotes = vMap.entryNotes();
        for (Map.Entry<SimpleValue, SimpleValue> e : vMap.valueMap().entrySet()) {
            SimpleValue key = e.getKey();
            SimpleValue value = e.getValue();

            JSONObject entryJson = new JSONObject(2);
            entryJson.put("$type", "$entry");
            entryJson.put("key", toJson(key));
            entryJson.put("value", toJson(value));
            if (entryEmbeds != null && entryEmbeds.containsKey(key)) {
                entryJson.put("$embed_value", entryEmbeds.get(key)); // 透传cfgeditor写入的entry value字段嵌入状态
            }
            if (foldedEntries != null && foldedEntries.contains(key)) {
                entryJson.put("$fold", true); // 透传cfgeditor写入的entry节点级折叠状态
            }
            if (entryNotes != null && entryNotes.containsKey(key)) {
                entryJson.put("$note", entryNotes.get(key)); // 透传cfgeditor写入的entry备注
            }
            json.add(entryJson);
        }
        return json;
    }
}
