package configgen.value;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import configgen.gen.TextI18n;
import configgen.schema.*;
import configgen.util.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static configgen.data.CfgData.DCell;
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
    private final boolean isLogNotFoundField;
    private DCell cell;
    private List<DCell> cellList;
    private String fromFileName;

    public ValueJsonParser(TableSchema subTableSchema) {
        this(subTableSchema, null);
    }

    public ValueJsonParser(TableSchema subTableSchema, TextI18n.TableI18n nullableTableI18n) {
        this(subTableSchema, nullableTableI18n, Logger.isWarningEnabled());
    }

    public ValueJsonParser(TableSchema subTableSchema, TextI18n.TableI18n nullableTableI18n, boolean isLogNotFoundField) {
        this.subTableSchema = subTableSchema;
        this.nullableTableI18n = nullableTableI18n;
        this.isLogNotFoundField = isLogNotFoundField;
    }

    public VStruct fromJson(String jsonStr, String fromFileName) {
        this.fromFileName = fromFileName;
        JSONObject jsonObject = JSON.parseObject(jsonStr);
        cell = DCell.EMPTY;
        cellList = List.of(cell);
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
        VStruct vStruct = new VStruct(subStructural, new ArrayList<>(subStructural.fields().size()), cellList);
        for (FieldSchema fs : subStructural.fields()) {
            Object fieldObj = jsonObject.get(fs.name());
            Value fieldValue;
            if (fieldObj != null) {
                fieldValue = parse(fs.type(), fieldObj);
            } else {
                // not throw exception, but use default value
                // make it easy to add field in future
                fieldValue = ValueDefault.of(fs.type());
                if (isLogNotFoundField) {
                    Logger.log("%s %s[%s] not found ", fromFileName, subStructural.fullName(), fs.name());
                }
            }
            vStruct.values().add(fieldValue);
        }
        return vStruct;
    }

    private VInterface parseInterface(InterfaceSchema subInterfaceSchema, JSONObject jsonObject) {
        String typeFullName = (String) jsonObject.get("$type");
        if (typeFullName == null) {
            throw new JsonParseException("$type not set");
        }
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
        return new VInterface(subInterfaceSchema, implValue, cellList); // 需要这层包装，以方便生成data file
    }

    private Value parse(FieldType type, Object obj) {
        switch (type) {
            case BOOL -> {
                boolean bv = switch (obj) {
                    case Boolean b -> b;
                    case Number num -> num.intValue() == 1;
                    default -> (boolean) obj;
                };
                return new VBool(bv, cell);
            }
            case INT -> {
                int iv = switch (obj) {
                    case Number num -> num.intValue();
                    default -> (int) obj;
                };
                return new VInt(iv, cell);
            }
            case LONG -> {
                long lv = switch (obj) {
                    case Number num -> num.longValue();
                    default -> (long) obj;
                };
                return new VLong(lv, cell);
            }
            case FLOAT -> {
                float fv = switch (obj) {
                    case Number num -> num.floatValue();
                    default -> (float) obj;
                };
                return new VFloat(fv, cell);
            }
            case STRING -> {
                return new VString((String) obj, cell);
            }
            case TEXT -> {
                String original = (String) obj;
                String i18n = null;
                String value;
                if (nullableTableI18n != null) {
                    i18n = nullableTableI18n.findText(original);
                    value = i18n != null ? i18n : original;
                } else {
                    value = original;
                }
                return new VText(value, original, i18n, cell);
            }
            case FList fList -> {
                JSONArray jsonArray = (JSONArray) obj;
                VList vList = new VList(new ArrayList<>(jsonArray.size()), cellList);
                for (Object itemObj : jsonArray) {
                    SimpleValue v = (SimpleValue) parse(fList.item(), itemObj);
                    vList.valueList().add(v);
                }
                return vList;
            }
            case FMap fMap -> {
                JSONArray jsonArray = (JSONArray) obj;
                int cnt = jsonArray.size();
                VMap vMap = new VMap(new LinkedHashMap<>(cnt), cellList);
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
