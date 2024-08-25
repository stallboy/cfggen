package configgen.value;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import configgen.ctx.TextI18n;
import configgen.data.Source;
import configgen.schema.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

import static configgen.schema.FieldType.*;
import static configgen.schema.FieldType.Primitive.*;
import static configgen.value.CfgValue.*;

public class ValueJsonParser {

    public static class JsonParseException extends RuntimeException {
        public JsonParseException(String message) {
            super(message);
        }
    }

    private final TableSchema subTableSchema;
    private final TextI18n.TableI18n nullableTableI18n;
    private Source.DFile source;

    public ValueJsonParser(TableSchema subTableSchema) {
        this(subTableSchema, null);
    }

    public ValueJsonParser(TableSchema subTableSchema, TextI18n.TableI18n nullableTableI18n) {
        this.subTableSchema = subTableSchema;
        this.nullableTableI18n = nullableTableI18n;
    }

    public VStruct fromJson(String jsonStr, String jsonFileName) {
        JSONObject jsonObject = JSON.parseObject(jsonStr);
        source = Source.of(jsonFileName);
        return parseStructural(subTableSchema, jsonObject);
    }

    private CompositeValue parseNameable(Nameable subNameable, JSONObject jsonObject) {
        switch (subNameable) {
            case InterfaceSchema interfaceSchema -> {
                return parseInterface(interfaceSchema, jsonObject);
            }
            case Structural structural -> {
                return parseStructural(structural, jsonObject);
            }
        }
    }

    private VStruct parseStructural(Structural subStructural, JSONObject jsonObject) {
        VStruct vStruct = new VStruct(subStructural, new ArrayList<>(subStructural.fields().size()), source);
        for (FieldSchema fs : subStructural.fields()) {
            Object fieldObj = jsonObject.get(fs.name());
            Value fieldValue;
            if (fieldObj != null) {
                fieldValue = parse(fs.type(), fieldObj);
            } else {
                // not throw exception, but use default value
                // save compactly and make it easy to add field in future
                fieldValue = ValueDefault.of(fs.type(), source);
//                if (isLogNotFoundField) {
//                    Logger.log("%s %s[%s] not found ", fromFileName, subStructural.fullName(), fs.name());
//                }
            }
            vStruct.values().add(fieldValue);
        }
        String note = (String) jsonObject.get("$note");
        if (note != null && !note.isEmpty()) {
            vStruct.setNote(note);
        }
        boolean fold = parseBool(jsonObject.get("$fold"));
        if (fold) {
            vStruct.setFold(true);
        }
        return vStruct;
    }

    private VInterface parseInterface(InterfaceSchema interfaceSchema, JSONObject jsonObject) {

        String typeFullName = (String) jsonObject.get("$type");
        String name = interfaceSchema.name();
        if (typeFullName == null) {
            throw new JsonParseException(" $type not set for " + name);
        }
        String interfaceNamePrefix = name + ".";
        if (!typeFullName.startsWith(interfaceNamePrefix)) {
            throw new JsonParseException(typeFullName + " not startWith " + name);
        }

        String implName = typeFullName.substring(interfaceNamePrefix.length());
        StructSchema impl = interfaceSchema.findImpl(implName);
        if (impl == null) {
            throw new JsonParseException(implName + " not found in interface " + name);
        }

        VStruct implValue = parseStructural(impl, jsonObject);
        return new VInterface(interfaceSchema, implValue, source); // 需要这层包装，以方便生成data file
    }

    private boolean parseBool(Object obj) {
        if (obj == null) {
            return false;
        }
        return switch (obj) {
            case Boolean b -> b;
            case Number num -> num.intValue() == 1;
            default -> (boolean) obj;
        };
    }

    private Value parse(FieldType type, Object obj) {
        switch (type) {
            case BOOL -> {
                return new VBool(parseBool(obj), source);
            }
            case INT -> {
                int iv = switch (obj) {
                    case Number num -> num.intValue();
                    default -> (int) obj;
                };
                return new VInt(iv, source);
            }
            case LONG -> {
                long lv = switch (obj) {
                    case Number num -> num.longValue();
                    default -> (long) obj;
                };
                return new VLong(lv, source);
            }
            case FLOAT -> {
                float fv = switch (obj) {
                    case Number num -> num.floatValue();
                    default -> (float) obj;
                };
                return new VFloat(fv, source);
            }
            case STRING -> {
                return new VString((String) obj, source);
            }
            case TEXT -> {
                String original = (String) obj;
                return ValueUtil.createText(original, source, nullableTableI18n);
            }
            case FList fList -> {
                JSONArray jsonArray = (JSONArray) obj;
                VList vList = new VList(new ArrayList<>(jsonArray.size()), source);
                for (Object itemObj : jsonArray) {
                    SimpleValue v = (SimpleValue) parse(fList.item(), itemObj);
                    vList.valueList().add(v);
                }
                return vList;
            }
            case FMap fMap -> {
                JSONArray jsonArray = (JSONArray) obj;
                int cnt = jsonArray.size();
                VMap vMap = new VMap(new LinkedHashMap<>(cnt), source);
                for (int i = 0; i < cnt; i++) {
                    JSONObject entry = jsonArray.getJSONObject(i);
                    Object keyObj = entry.get("key");
                    Object valueObj = entry.get("value");
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
