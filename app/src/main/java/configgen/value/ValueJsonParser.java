package configgen.value;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import configgen.data.Source;
import configgen.schema.*;

import java.util.*;

import static configgen.data.Source.*;
import static configgen.schema.FieldType.*;
import static configgen.schema.FieldType.Primitive.*;
import static configgen.value.CfgValue.*;
import static configgen.value.CfgValueErrs.*;

public class ValueJsonParser {

    private final TableSchema tableSchema;
    private final boolean isTableSchemaPartial;
    private final CfgValueErrs errs;

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
        try {
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            if (jsonObject != null) {
                return parseStructural(tableSchema, jsonObject, source);
            } else {
                errs.addErr(new JsonStrEmpty(source));
            }
        } catch (JSONException e) {
            errs.addErr(new JsonParseException(source, e.getMessage()));
        }
        return ValueDefault.ofStructural(tableSchema, source);
    }


    private CompositeValue parseNameable(Nameable nameable, JSONObject jsonObject, DFile source) {
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

    private VStruct parseStructural(Structural structural, JSONObject jsonObject, DFile source) {
        String type = jsonObject.getString("$type");
        if (type == null) {
//            errs.addErr(new JsonTypeNotExist(source, structural.fullName()));
//            return ValueDefault.ofStructural(structural, source);
            type = structural.fullName(); //兼容一点，可以没有
        } else if (!type.equals(structural.fullName())) {
            errs.addErr(new JsonTypeNotMatch(source, type, structural.fullName()));
            return ValueDefault.ofStructural(structural, source);
        }


        VStruct vStruct = new VStruct(structural, new ArrayList<>(structural.fields().size()), source);
        DFile thisSource = source.inStruct(structural.fullName());
        for (FieldSchema fs : structural.fields()) {
            Object fieldObj = jsonObject.get(fs.name());
            Value fieldValue;

            DFile fieldSource = thisSource.child(fs.name());
            if (fieldObj != null) {
                fieldValue = parse(fs.type(), fieldObj, fieldSource, fs);
            } else {
                // not throw exception, but use default value
                // save compactly and make it easy to add field in future
                fieldValue = ValueDefault.of(fs.type(), fieldSource);
//                if (isLogNotFoundField) {
//                    Logger.log("%s %s[%s] not found ", fromFileName, subStructural.fullName(), fs.name());
//                }
            }
            vStruct.values().add(fieldValue);
        }

        String note = jsonObject.getString("$note");
        if (note != null && !note.isEmpty()) {
            vStruct.setNote(note);
        }
        boolean fold = parseBool(jsonObject.get("$fold"), thisSource.child("$fold"));
        if (fold) {
            vStruct.setFold(true);
        }

        if (!isTableSchemaPartial) {
            Set<String> jsonKeys = new HashSet<>(jsonObject.keySet());
            jsonKeys.removeAll(structural.fieldNameSet());
            jsonKeys.removeAll(jsonExtraKeySet);
            if (!jsonKeys.isEmpty()) {
                errs.addWarn(new JsonHasExtraFields(thisSource, type, jsonKeys));
            }
        }

        return vStruct;
    }

    private VInterface parseInterface(InterfaceSchema interfaceSchema, JSONObject jsonObject, DFile source) {
        String type = jsonObject.getString("$type");
        String name = interfaceSchema.name();
        if (type == null) {
            errs.addErr(new JsonTypeNotExist(source, name));
            return ValueDefault.ofInterface(interfaceSchema, source);
        }
        String interfaceNamePrefix = name + ".";
        if (!type.startsWith(interfaceNamePrefix)) {
            errs.addErr(new JsonTypeNotMatch(source, type, name));
            return ValueDefault.ofInterface(interfaceSchema, source);
        }

        String implName = type.substring(interfaceNamePrefix.length());
        StructSchema impl = interfaceSchema.findImpl(implName);
        if (impl == null) {
            errs.addErr(new JsonTypeNotMatch(source, type, name));
            return ValueDefault.ofInterface(interfaceSchema, source);
        }

        VStruct implValue = parseStructural(impl, jsonObject, source.lastAppend("<" + implName + ">"));
        return new VInterface(interfaceSchema, implValue, source); // 需要这层包装，以方便生成data file
    }

    private boolean parseBool(Object obj, DFile source) {
        if (obj == null) {
            return false;
        }
        switch (obj) {
            case Boolean b -> {
                return b;
            }
            case Number num -> {
                return num.intValue() == 1;
            }
            default -> {
                errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.BOOL));
                return false;
            }
        }
    }

    private Value parse(FieldType type, Object obj, DFile source, FieldSchema fieldSchema) {
        switch (type) {
            case BOOL -> {
                return new VBool(parseBool(obj, source), source);
            }
            case INT -> {
                int iv = 0;
                switch (obj) {
                    case Number num -> {
                        iv = num.intValue();
                    }
                    default -> {
                        errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.INT));
                    }
                }
                return new VInt(iv, source);
            }
            case LONG -> {
                long lv = 0;
                switch (obj) {
                    case Number num -> {
                        lv = num.longValue();
                    }
                    default -> {
                        errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.LONG));
                    }
                }
                return new VLong(lv, source);
            }
            case FLOAT -> {
                float fv = 0;
                switch (obj) {
                    case Number num -> {
                        fv = num.floatValue();
                    }
                    default -> {
                        errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.FLOAT));
                    }
                }
                return new VFloat(fv, source);
            }
            case STRING -> {
                String sv = "";
                switch (obj) {
                    case String str -> {
                        sv = str;
                        if (fieldSchema.meta().isLowercase()){
                            sv = sv.toLowerCase();
                        }
                    }
                    default -> {
                        errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.STR));
                    }
                }
                return new VString(sv, source);
            }
            case TEXT -> {
                String sv = "";
                switch (obj) {
                    case String str -> {
                        sv = str;
                        if (fieldSchema.meta().isLowercase()){
                            sv = sv.toLowerCase();
                        }
                    }
                    default -> {
                        errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.STR));
                    }
                }
                return new VText(sv, source);
            }
            case FList fList -> {
                JSONArray jsonArray = JSONArray.of();
                switch (obj) {
                    case JSONArray array -> {
                        jsonArray = array;
                    }
                    default -> {
                        errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.ARRAY));
                    }
                }

                VList vList = new VList(new ArrayList<>(jsonArray.size()), source);
                int i = 0;
                for (Object itemObj : jsonArray) {
                    Value v = parse(fList.item(), itemObj, source.child("[" + i + "]"), fieldSchema);
                    if (v instanceof SimpleValue sv) {
                        vList.valueList().add(sv);
                    }   // else里的异常，会被parseNameable记录

                    i++;
                }
                return vList;
            }
            case FMap fMap -> {
                JSONArray jsonArray = JSONArray.of();
                switch (obj) {
                    case JSONArray array -> {
                        jsonArray = array;
                    }
                    default -> {
                        errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.MAP));
                    }
                }
                int cnt = jsonArray.size();
                VMap vMap = new VMap(new LinkedHashMap<>(cnt), source);
                int i = 0;
                for (Object itemObj : jsonArray) {
                    JSONObject entry = null;
                    switch (itemObj) {
                        case JSONObject jsonObject -> {
                            entry = jsonObject;
                        }
                        default -> {
                            errs.addErr(new JsonValueNotMatchType(source.child("[e" + i + "]"), itemObj.toString(), EType.MAP_ENTRY));
                        }
                    }
                    if (entry != null) {
                        Object keyObj = entry.get("key");
                        DFile ks = source.child("[k" + i + "]");
                        if (keyObj == null) {
                            errs.addErr(new JsonValueNotMatchType(ks, itemObj.toString(), EType.MAP_ENTRY));
                        }

                        Object valueObj = entry.get("value");
                        DFile vs = source.child("[v" + i + "]");
                        if (valueObj == null) {
                            errs.addErr(new JsonValueNotMatchType(vs, itemObj.toString(), EType.MAP_ENTRY));
                        }

                        if (keyObj != null && valueObj != null) {
                            Value key = parse(fMap.key(), keyObj, ks, fieldSchema);
                            Value value = parse(fMap.value(), valueObj, vs, fieldSchema);
                            if (key instanceof SimpleValue kk && value instanceof SimpleValue vv) {
                                vMap.valueMap().put(kk, vv);
                            } // else里的异常，会被parseNameable记录
                        }
                    }
                    i++;
                }
                return vMap;
            }
            case StructRef structRef -> {
                JSONObject ov = null;
                switch (obj) {
                    case JSONObject jsonObject -> {
                        ov = jsonObject;
                    }
                    default -> {
                        errs.addErr(new JsonValueNotMatchType(source, obj.toString(), EType.STRUCT));
                    }
                }
                if (ov != null) {
                    return parseNameable(structRef.obj(), ov, source);
                } else {
                    return ValueDefault.ofNamable(structRef.obj(), source);
                }
            }
        }
    }

}
