package configgen.value;

import configgen.data.Source;
import configgen.schema.*;
import org.simdjson.JsonValue;
import org.simdjson.SimdJsonParser;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static configgen.data.Source.*;
import static configgen.schema.FieldType.*;
import static configgen.schema.FieldType.Primitive.*;
import static configgen.value.CfgValue.*;
import static configgen.value.CfgValueErrs.*;

@SuppressWarnings("SwitchStatementWithTooFewBranches")
public class ValueJsonParser {
    private final TableSchema tableSchema;
    private final boolean isTableSchemaPartial;
    private final CfgValueErrs errs;

    /**
     * simdjson 的 SimdJsonParser 内部复用缓冲区、非线程安全；cfggen 用 ForkJoinPool 并发读多张表，
     * 故按线程各持有一个 parser，跨多次解析复用（simdjson 的标准用法：one parser, many documents）。
     */
    private static final ThreadLocal<SimdJsonParser> PARSER = ThreadLocal.withInitial(SimdJsonParser::new);

    public ValueJsonParser(TableSchema tableSchema,
                           CfgValueErrs errs) {
        this(tableSchema, false, errs);
    }

    public ValueJsonParser(TableSchema tableSchema,
                           boolean isTableSchemaPartial,
                           CfgValueErrs errs) {
        this.tableSchema = tableSchema;
        this.isTableSchemaPartial = isTableSchemaPartial;
        this.errs = errs;
    }

    public VStruct fromJson(String jsonStr) {
        return fromJson(jsonStr, Source.DFile.of("<server>", tableSchema.name()));
    }

    public VStruct fromJson(String jsonStr, DFile source) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            errs.addErr(new JsonStrEmpty(source));
            return ValueDefault.ofStructural(tableSchema, source);
        }
        return fromJson(jsonStr.getBytes(StandardCharsets.UTF_8), source);
    }

    /**
     * 直接吃 UTF-8 字节，避免上层先解码成 String。热路径（VTableJsonParser 读数据文件）走这里。
     */
    public VStruct fromJson(byte[] jsonBytes, DFile source) {
        // simdjson 严格 UTF-8，不跳 BOM；部分 Windows 生成的 json 文件带 UTF-8 BOM（EF BB BF），
        // 需先剥离，否则报 "Unrecognized primitive"。fastjson2 原本会自行跳过。
        int len = jsonBytes.length;
        if (len >= 3
                && (jsonBytes[0] & 0xFF) == 0xEF
                && (jsonBytes[1] & 0xFF) == 0xBB
                && (jsonBytes[2] & 0xFF) == 0xBF) {
            jsonBytes = Arrays.copyOfRange(jsonBytes, 3, len);
            len = jsonBytes.length;
        }
        try {
            JsonValue root = PARSER.get().parse(jsonBytes, len);
            if (root != null && root.isObject()) {
                return parseStructural(tableSchema, root, source);
            } else {
                errs.addErr(new JsonStrEmpty(source));
            }
        } catch (Exception e) {
            errs.addErr(new JsonParseException(source, e.getMessage()));
        }
        return ValueDefault.ofStructural(tableSchema, source);
    }


    private SimpleValue parseNameable(Nameable nameable, JsonValue jsonObject, DFile source) {
        switch (nameable) {
            case InterfaceSchema interfaceSchema -> {
                return parseInterface(interfaceSchema, jsonObject, source);
            }
            case Structural structural -> {
                return parseStructural(structural, jsonObject, source);
            }
        }
    }

    private static final Set<String> jsonExtraKeySet = Set.of("$type", "$note", "$fold", "$refs");

    private VStruct parseStructural(Structural structural, JsonValue jsonObject, DFile source) {
        String type = structural.fullName();

        VStruct vStruct = new VStruct(structural, new ArrayList<>(structural.fields().size()), source);
        DFile thisSource = source.inStruct(structural.fullName());
        for (FieldSchema fs : structural.fields()) {
            JsonValue fieldObj = jsonObject.get(fs.name());
            Value fieldValue;

            DFile fieldSource = thisSource.child(fs.name());
            if (fieldObj != null && !fieldObj.isNull()) {
                fieldValue = parse(fs.type(), fieldObj, fieldSource, fs);
            } else {
                // not throw exception, but use default value
                // save compactly and make it easy to add field in future
                fieldValue = ValueDefault.of(fs.type(), fieldSource);
            }
            vStruct.values().add(fieldValue);
        }

        String note = getString(jsonObject, "$note");
        if (note != null && !note.isEmpty()) {
            vStruct.setNote(note);
        }
        boolean fold = parseBool(jsonObject.get("$fold"), thisSource.child("$fold"));
        if (fold) {
            vStruct.setFold(true);
        }

        if (!isTableSchemaPartial) {
            Set<String> jsonKeys = new HashSet<>();
            Iterator<Map.Entry<String, JsonValue>> it = jsonObject.objectIterator();
            while (it.hasNext()) {
                jsonKeys.add(it.next().getKey());
            }
            jsonKeys.removeAll(structural.fieldNameSet());
            jsonKeys.removeAll(jsonExtraKeySet);
            if (!jsonKeys.isEmpty()) {
                errs.addWarn(new JsonHasExtraFields(thisSource, type, jsonKeys));
            }
        }

        return vStruct;
    }

    private VInterface parseInterface(InterfaceSchema interfaceSchema, JsonValue jsonObject, DFile source) {
        String type = getString(jsonObject, "$type");
        String name = interfaceSchema.name();
        if (type == null) {
            errs.addErr(new JsonTypeNotExist(source, name));
            return ValueDefault.ofInterface(interfaceSchema, source);
        }
        String implName;
        String interfaceNamePrefix = name + ".";
        if (type.contains(".")) {
            if (type.startsWith(interfaceNamePrefix)) {  // 老版本数据，是包含了前缀的，兼容
                implName = type.substring(interfaceNamePrefix.length());
            } else {
                errs.addErr(new JsonTypeNotMatch(source, type, name));
                return ValueDefault.ofInterface(interfaceSchema, source);
            }
        } else {
            implName = type; // 建议这种，不用包含前缀
        }

        StructSchema impl = interfaceSchema.findImpl(implName);
        if (impl == null) {
            errs.addErr(new JsonTypeNotMatch(source, type, name));
            return ValueDefault.ofInterface(interfaceSchema, source);
        }

        VStruct implValue = parseStructural(impl, jsonObject, source.lastAppend("<" + implName + ">"));
        return new VInterface(interfaceSchema, implValue, source); // 需要这层包装，以方便生成data file
    }

    private boolean parseBool(JsonValue obj, DFile source) {
        if (obj == null || obj.isNull()) {
            return false;
        }
        if (obj.isBoolean()) {
            return obj.asBoolean();
        }
        if (obj.isLong()) {
            return obj.asLong() == 1;
        }
        errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.BOOL));
        return false;
    }

    private SimpleValue parseSimpleType(SimpleType type, JsonValue obj, DFile source, FieldSchema fieldSchema) {
        switch (type) {
            case BOOL -> {
                return new VBool(parseBool(obj, source), source);
            }
            case INT -> {
                int iv = 0;
                if (obj != null && !obj.isNull() && (obj.isLong() || obj.isDouble())) {
                    iv = obj.isLong() ? (int) obj.asLong() : (int) obj.asDouble();
                } else if (obj != null && !obj.isNull()) {
                    errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.INT));
                }
                return new VInt(iv, source);
            }
            case LONG -> {
                long lv = 0;
                if (obj != null && !obj.isNull() && (obj.isLong() || obj.isDouble())) {
                    lv = obj.isLong() ? obj.asLong() : (long) obj.asDouble();
                } else if (obj != null && !obj.isNull()) {
                    errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.LONG));
                }
                return new VLong(lv, source);
            }
            case FLOAT -> {
                float fv = 0;
                if (obj != null && !obj.isNull() && (obj.isLong() || obj.isDouble())) {
                    fv = obj.isLong() ? (float) obj.asLong() : (float) obj.asDouble();
                } else if (obj != null && !obj.isNull()) {
                    errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.FLOAT));
                }
                return new VFloat(fv, source);
            }
            case STRING -> {
                String sv = "";
                if (obj != null && !obj.isNull() && obj.isString()) {
                    sv = obj.asString();
                    if (fieldSchema.isLowercase()) {
                        sv = sv.toLowerCase();
                    }
                } else if (obj != null && !obj.isNull()) {
                    errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.STR));
                }
                return new VString(sv, source);
            }
            case TEXT -> {
                String sv = "";
                if (obj != null && !obj.isNull() && obj.isString()) {
                    sv = obj.asString();
                    if (fieldSchema.isLowercase()) {
                        sv = sv.toLowerCase();
                    }
                } else if (obj != null && !obj.isNull()) {
                    errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.STR));
                }
                return new VText(sv, source);
            }
            case StructRef structRef -> {
                if (obj != null && !obj.isNull() && obj.isObject()) {
                    return parseNameable(structRef.obj(), obj, source);
                } else {
                    errs.addErr(new JsonValueNotMatchType(source, obj == null ? "null" : obj.toString(), EType.STRUCT));
                    return ValueDefault.ofNamable(structRef.obj(), source);
                }
            }
        }

    }

    private Value parse(FieldType type, JsonValue obj, DFile source, FieldSchema fieldSchema) {
        switch (type) {
            case SimpleType simpleType -> {
                return parseSimpleType(simpleType, obj, source, fieldSchema);
            }

            case FList fList -> {
                if (obj == null || obj.isNull() || !obj.isArray()) {
                    if (obj != null && !obj.isNull()) {
                        errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.ARRAY));
                    }
                    return new VList(new ArrayList<>(), source);
                }

                VList vList = new VList(new ArrayList<>(obj.getSize()), source);
                int i = 0;
                Iterator<JsonValue> it = obj.arrayIterator();
                while (it.hasNext()) {
                    JsonValue itemObj = it.next();
                    Value v = parse(fList.item(), itemObj, source.child("[" + i + "]"), fieldSchema);
                    if (v instanceof SimpleValue sv) {
                        vList.valueList().add(sv);
                    }   // else里的异常，会被parseNameable记录

                    i++;
                }
                return vList;
            }
            case FMap fMap -> {
                if (obj == null || obj.isNull() || !obj.isArray()) {
                    if (obj != null && !obj.isNull()) {
                        errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.MAP));
                    }
                    return new VMap(new LinkedHashMap<>(), source);
                }
                VMap vMap = new VMap(new LinkedHashMap<>(obj.getSize()), source);
                int i = 0;
                Iterator<JsonValue> it = obj.arrayIterator();
                while (it.hasNext()) {
                    JsonValue itemObj = it.next();
                    DFile ks = source.child("[k" + i + "]");
                    DFile vs = source.child("[v" + i + "]");
                    if (!itemObj.isObject()) {
                        errs.addErr(new JsonValueNotMatchType(source.child("[e" + i + "]"), itemObj.toString(), EType.MAP_ENTRY));
                        i++;
                        continue;
                    }

                    JsonValue keyObj = itemObj.get("key");
                    JsonValue valueObj = itemObj.get("value");
                    if (keyObj == null || keyObj.isNull()) {
                        errs.addErr(new JsonValueNotMatchType(ks, itemObj.toString(), EType.MAP_ENTRY));
                    }
                    if (valueObj == null || valueObj.isNull()) {
                        errs.addErr(new JsonValueNotMatchType(vs, itemObj.toString(), EType.MAP_ENTRY));
                    }

                    if (keyObj != null && !keyObj.isNull() && valueObj != null && !valueObj.isNull()) {
                        SimpleValue key = parseSimpleType(fMap.key(), keyObj, ks, fieldSchema);
                        SimpleValue value = parseSimpleType(fMap.value(), valueObj, vs, fieldSchema);
                        vMap.valueMap().put(key, value);
                    }
                    i++;
                }
                return vMap;
            }

        }
    }

    private static String getString(JsonValue obj, String key) {
        if (obj == null) {
            return null;
        }
        JsonValue v = obj.get(key);
        return (v != null && !v.isNull() && v.isString()) ? v.asString() : null;
    }

}
