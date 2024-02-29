package configgen.value;

import configgen.gen.Generator;
import configgen.schema.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;

public class ValueRefCollector {

    public record RefId(String table,
                        String id) {
    }

    public record FieldRef(
            String firstField,
            String label,
            String toTable,
            String toId) {
    }

    private final CfgValue cfgValue;
    private final Map<RefId, VStruct> refIdToRecordMap;
    private final List<FieldRef> fieldRefs;

    public static List<FieldRef> collectRefs(Value record, CfgValue cfgValue) {
        List<FieldRef> fieldRefs = new ArrayList<>();
        Map<RefId, VStruct> newFrontier = new LinkedHashMap<>();
        ValueRefCollector collector = new ValueRefCollector(cfgValue, newFrontier, fieldRefs);
        collector.collect(record, List.of());
        return fieldRefs;
    }

    public ValueRefCollector(CfgValue cfgValue, Map<RefId, VStruct> refIdToRecordMap, List<FieldRef> fieldRefs) {
        this.cfgValue = cfgValue;
        this.refIdToRecordMap = refIdToRecordMap;
        this.fieldRefs = fieldRefs;
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
        String pre = "";
        if (!prefix.isEmpty()) {
            pre = String.join(".", prefix) + ".";
        }

        collectStructRef(cfgValue, vStruct, refIdToRecordMap, fieldRefs, pre);

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

    static List<FieldRef> collectStructRef(CfgValue cfgValue,
                                           VStruct vStruct,
                                           Map<RefId, VStruct> refIdToRecordMap,
                                           List<FieldRef> fieldRefs,
                                           String namePrefix
    ) {
        if (fieldRefs == null) {
            fieldRefs = new ArrayList<>();
        }

        Structural structural = vStruct.schema();
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            RefKey refKey = fk.refKey();
            Map<Value, VStruct> foreignKeyValueMap = ValueUtil.getForeignKeyValueMap(cfgValue, fk);
            if (foreignKeyValueMap == null) {
                continue;
            }
            // 这里没考虑ListRef
            if (refKey instanceof RefKey.RefSimple refSimple) {
                FieldSchema firstField = fk.key().fieldSchemas().getFirst();
                FieldType ft = firstField.type();

                // 允许自定义ref的名称
                String refName = null;
                String refTitleFieldName = fk.meta().getStr("refTitle", null);
                if (refTitleFieldName != null) {
                    Value refTitleValue = ValueUtil.extractFieldValue(vStruct, refTitleFieldName);
                    if (refTitleValue instanceof StringValue stringValue) {
                        refName = stringValue.value();
                    }
                }
                if (refName == null) {
                    refName = namePrefix + (refSimple.nullable() ? "nullableRef" : "ref") + Generator.upper1(fk.name());
                }

                switch (ft) {
                    case FieldType.SimpleType ignored -> {
                        Value localValue = ValueUtil.extractKeyValue(vStruct, fk.keyIndices());
                        VStruct refRecord = foreignKeyValueMap.get(localValue);
                        if (refRecord != null) {
                            addRef(refIdToRecordMap, fieldRefs, fk, refName, localValue.packStr(), refRecord);
                        }
                    }
                    case FieldType.FList ignored -> {
                        VList localList = (VList) vStruct.values().get(fk.keyIndices()[0]);
                        for (SimpleValue item : localList.valueList()) {
                            VStruct refRecord = foreignKeyValueMap.get(item);
                            if (refRecord != null) {
                                addRef(refIdToRecordMap, fieldRefs, fk, refName, item.packStr(), refRecord);
                            }
                        }
                    }
                    case FieldType.FMap ignored -> {
                        VMap localMap = (VMap) vStruct.values().get(fk.keyIndices()[0]);
                        for (SimpleValue val : localMap.valueMap().values()) {
                            VStruct refRecord = foreignKeyValueMap.get(val);
                            if (refRecord != null) {
                                addRef(refIdToRecordMap, fieldRefs, fk, refName, val.packStr(), refRecord);
                            }
                        }
                    }
                }
            }
        }
        return fieldRefs;
    }

    private static void addRef(Map<RefId, VStruct> refIdToRecordMap, List<FieldRef> fieldRefs, ForeignKeySchema fk, String refName, String id, VStruct refRecord) {
        String table = fk.refTableNormalized();
        RefId refId = new RefId(table, id);
        refIdToRecordMap.put(refId, refRecord);
        fieldRefs.add(new FieldRef(fk.key().fieldSchemas().getFirst().name(), refName, table, id));
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
