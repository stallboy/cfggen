package configgen.value;

import configgen.gen.Generator;
import configgen.schema.FieldType;
import configgen.schema.ForeignKeySchema;
import configgen.schema.RefKey;
import configgen.schema.Structural;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;

public class ValueRefCollector {

    public record RefId(String table,
                        String id) {
    }

    private final Map<RefId, VStruct> refRecordMap;
    private final Map<String, List<RefId>> refIdMap;


    public ValueRefCollector(Map<RefId, VStruct> refRecordMap, Map<String, List<RefId>> refIdMap) {
        this.refRecordMap = refRecordMap;
        this.refIdMap = refIdMap;
    }

    public void collect(Value value, List<String> prefix) {
        switch (value) {
            case VStruct vStruct -> collect(vStruct, prefix);
            case VInterface vInterface -> collect(vInterface, prefix);
            case VList vList -> collect(vList, prefix);
            case VMap vMap -> collect(vMap, prefix);
            default -> {
            }
        }
    }

    public void collect(VStruct vStruct, List<String> prefix) {
        Map<String, List<RefId>> thisRefIdMap = collectStructRef(refRecordMap, vStruct);
        if (prefix.isEmpty()) {
            refIdMap.putAll(thisRefIdMap);
        } else {
            for (Map.Entry<String, List<RefId>> e : thisRefIdMap.entrySet()) {
                refIdMap.put(String.join(".", prefix) + "." + e.getKey(), e.getValue());
            }
        }

        int i = 0;
        for (Value value : vStruct.values()) {
            String name = vStruct.schema().fields().get(i).name();
            collect(value, listAddOf(prefix, name));
            i++;
        }
    }

    private static List<String> listAddOf(List<String> old, String e) {
        List<String> res = new ArrayList<>(old.size() + 1);
        res.addAll(old);
        res.add(e);
        return res;
    }

    static Map<String, List<RefId>> collectStructRef(Map<RefId, VStruct> refRecordMap, VStruct vStruct) {
        Map<String, List<RefId>> refIdMap = new LinkedHashMap<>();
        Structural structural = vStruct.schema();
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            RefKey refKey = fk.refKey();
            // TODO, 这里没考虑ListRef
            if (refKey instanceof RefKey.RefSimple refSimple) {
                FieldType ft = fk.key().fieldSchemas().getFirst().type();
                String refName = (refSimple.nullable() ? "nullableRef" : "ref") + Generator.upper1(fk.name());
                switch (ft) {
                    case FieldType.SimpleType ignored -> {
                        Value localValue = ValueUtil.extractKeyValue(vStruct, fk.keyIndices);
                        VStruct refRecord = fk.fkValueMap.get(localValue);
                        if (refRecord != null) {
                            addRef(refRecordMap, refIdMap, refName, fk.refTableNormalized(), localValue.packStr(), refRecord);
                        }
                    }
                    case FieldType.FList ignored -> {
                        VList localList = (VList) vStruct.values().get(fk.keyIndices[0]);
                        for (SimpleValue item : localList.valueList()) {
                            VStruct refRecord = fk.fkValueMap.get(item);
                            if (refRecord != null) {
                                addRef(refRecordMap, refIdMap, refName, fk.refTableNormalized(), item.packStr(), refRecord);
                            }
                        }
                    }
                    case FieldType.FMap ignored -> {
                        VMap localMap = (VMap) vStruct.values().get(fk.keyIndices[0]);
                        for (SimpleValue val : localMap.valueMap().values()) {
                            VStruct refRecord = fk.fkValueMap.get(val);
                            if (refRecord != null) {
                                addRef(refRecordMap, refIdMap, refName, fk.refTableNormalized(), val.packStr(), refRecord);
                            }
                        }
                    }
                }
            }
        }
        return refIdMap;
    }

    private static void addRef(Map<RefId, VStruct> refRecordMap, Map<String, List<RefId>> refIdMap, String refName, String table, String id, VStruct refRecord) {
        RefId refId = new RefId(table, id);
        refRecordMap.put(refId, refRecord);
        List<RefId> refIds = refIdMap.computeIfAbsent(refName, k -> new ArrayList<>());
        refIds.add(refId);
    }


    public void collect(VInterface vInterface, List<String> prefix) {
        VStruct child = vInterface.child();
        collect(child, prefix);
    }

    public void collect(VList vList, List<String> prefix) {
        int i = 0;
        for (SimpleValue sv : vList.valueList()) {
            collect(sv, listAddOf(prefix, String.valueOf(i)));
            i++;
        }
    }

    public void collect(VMap vMap, List<String> prefix) {
        int i = 0;
        for (Map.Entry<SimpleValue, SimpleValue> e : vMap.valueMap().entrySet()) {
            SimpleValue key = e.getKey();
            SimpleValue value = e.getValue();
            collect(key, listAddOf(prefix, i + "k"));
            collect(value, listAddOf(prefix, i + "v"));
            i++;
        }
    }


}
