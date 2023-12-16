package configgen.value;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import configgen.schema.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static configgen.data.CfgData.DCell;
import static configgen.schema.FieldType.*;
import static configgen.schema.FieldType.Primitive.*;
import static configgen.value.CfgValue.*;

public class ValueFromJson {

    public static class JsonParseException extends RuntimeException {
        public JsonParseException(String message) {
            super(message);
        }
    }

    private final TableSchema subTableSchema;
    private final TextI18n.TableI18n nullableTableI18n;

    public ValueFromJson(TableSchema subTableSchema, TextI18n.TableI18n nullableTableI18n) {
        this.subTableSchema = subTableSchema;
        this.nullableTableI18n = nullableTableI18n;
    }

    public VStruct fromJson(JSONObject jsonObject) {
        return parseStructural(subTableSchema, jsonObject);
    }

    private CompositeValue parseNameable(Nameable subNamable, JSONObject jsonObject) {
        switch (subNamable) {
            case InterfaceSchema interfaceSchema -> {
                return parseInterface(interfaceSchema, jsonObject);
            }
            case Structural structural -> {
                return parseStructural(structural, jsonObject);
            }
        }
    }

    private VStruct parseStructural(Structural subStructural, JSONObject jsonObject) {
        VStruct vStruct = new VStruct(subStructural, new ArrayList<>(subStructural.fields().size()), List.of());
        for (FieldSchema fs : subStructural.fields()) {
            Object fieldObj = jsonObject.get(fs.name());
            if (fieldObj == null) {
                throw new JsonParseException(fs.name() + " field not found in json object");
            }
            Value fieldValue = parse(fs.type(), fieldObj);
            vStruct.values().add(fieldValue);
        }
        return vStruct;
    }

    private VInterface parseInterface(InterfaceSchema subInterfaceSchema, JSONObject jsonObject) {
        String typeFullName = (String) jsonObject.get("$type");
        String interfaceNamePrefix = subInterfaceSchema.name() + ".";
        if (!typeFullName.startsWith(interfaceNamePrefix)) {
            throw new JsonParseException(typeFullName + " not found");
        }

        String implName = typeFullName.substring(interfaceNamePrefix.length());

        StructSchema impl = subInterfaceSchema.findImpl(implName);
        if (impl == null) {
            throw new JsonParseException(implName + " not found in interface");
        }

        VStruct implValue = parseStructural(impl, jsonObject);
        return new VInterface(subInterfaceSchema, implValue, List.of());
    }

    private Value parse(FieldType type, Object obj) {
        switch (type) {
            case BOOL -> {
                return new VBool((Boolean) obj, DCell.DUMMY);
            }
            case INT -> {
                return new VInt((Integer) obj, DCell.DUMMY);
            }
            case LONG -> {
                return new VLong((Long) obj, DCell.DUMMY);
            }
            case FLOAT -> {
                return new VFloat((Float) obj, DCell.DUMMY);
            }
            case STRING -> {
                return new VString((String) obj, DCell.DUMMY);
            }
            case TEXT -> {
                String str = (String) obj;
                String value;
                if (nullableTableI18n != null) {
                    value = nullableTableI18n.findText(str);
                    if (value == null) {
                        value = str;
                    }
                } else {
                    value = str;
                }
                return new VText(value, str, DCell.DUMMY);
            }
            case FList fList -> {
                JSONArray jsonArray = (JSONArray) obj;
                VList vList = new VList(new ArrayList<>(jsonArray.size()), List.of());
                for (Object itemObj : jsonArray) {
                    SimpleValue v = (SimpleValue) parse(fList.item(), itemObj);
                    vList.valueList().add(v);
                }
                return vList;
            }
            case FMap fMap -> {
                JSONArray jsonArray = (JSONArray) obj;
                int cnt = jsonArray.size();
                VMap vMap = new VMap(new LinkedHashMap<>(cnt), List.of());
                for (int i = 0; i < cnt / 2; i++) {
                    Object keyObj = jsonArray.get(i * 2);
                    Object valueObj = jsonArray.get(i * 2 + 1);
                    SimpleValue key = (SimpleValue) parse(fMap.key(), keyObj);
                    SimpleValue value = (SimpleValue) parse(fMap.value(), valueObj);
                    vMap.valueMap().put(key, value);
                }
                return vMap;
            }
            case StructRef structRef -> {
                return parseNameable(structRef.obj(), (JSONObject) obj);
            }
        }
    }

}
