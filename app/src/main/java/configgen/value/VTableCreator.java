package configgen.value;

import configgen.schema.*;

import java.util.*;

import static configgen.value.CfgValue.*;

public class VTableCreator {
    private final TableSchema tableSchema;
    private final CfgValueErrs errs;

    public VTableCreator(TableSchema tableSchema, CfgValueErrs errs) {
        this.tableSchema = tableSchema;
        this.errs = errs;
    }

    public VTable create(List<VStruct> valueList) {
        // 收集主键和唯一键
        SequencedMap<Value, VStruct> primaryKeyMap = new LinkedHashMap<>();
        SequencedMap<List<String>, SequencedMap<Value, VStruct>> uniqueKeyValueSetMap = new LinkedHashMap<>();
        extractKeyValues(primaryKeyMap, valueList, tableSchema.primaryKey());

        // 如果主键是int类型，则按主键int值排序，否则保持原顺序
        primaryKeyMap = sortIfPKIsNumber(primaryKeyMap);
        for (KeySchema uniqueKey : tableSchema.uniqueKeys()) {
            SequencedMap<Value, VStruct> ukMap = new LinkedHashMap<>();
            extractKeyValues(ukMap, primaryKeyMap.values(), uniqueKey);
            uniqueKeyValueSetMap.put(uniqueKey.fields(), ukMap);
        }

        // 收集枚举
        SequencedSet<String> enumNames = null;
        SequencedMap<String, Integer> enumNameToIntegerValueMap = null;
        if (tableSchema.entry() instanceof EntryType.EntryBase entry) {
            Set<String> names = new HashSet<>();
            int idx = FindFieldIndex.findFieldIndex(tableSchema, entry.fieldSchema());
            enumNames = new LinkedHashSet<>();

            int pkIdx = -1;
            List<FieldSchema> pk = tableSchema.primaryKey().fieldSchemas();
            if (pk.size() == 1 && pk.getFirst() != entry.fieldSchema()) {
                pkIdx = FindFieldIndex.findFieldIndex(tableSchema, pk.getFirst());
                enumNameToIntegerValueMap = new LinkedHashMap<>();
            }

            for (VStruct vStruct : valueList) {
                VString vStr = (VString) vStruct.values().get(idx);
                String e = vStr.value();
                if (e.contains(" ")) {
                    errs.addErr(new CfgValueErrs.EntryContainsSpace(vStr.source(), tableSchema.name()));
                    continue;
                }

                if (e.isEmpty()) {
                    if (entry instanceof EntryType.EEnum) {
                        errs.addErr(new CfgValueErrs.EnumEmpty(vStr.source(), tableSchema.name()));
                    }
                } else {
                    boolean add = names.add(e.toUpperCase());
                    if (!add) {
                        errs.addErr(new CfgValueErrs.EntryDuplicated(vStr.source(), tableSchema.name()));
                    } else {
                        enumNames.add(e);

                        if (pkIdx != -1) { //必须是int，这里是java生成需要
                            VInt vInt = (VInt) vStruct.values().get(pkIdx);
                            enumNameToIntegerValueMap.put(e, vInt.value());
                        }
                    }
                }
            }
        }

        return new VTable(tableSchema, valueList,
                primaryKeyMap, uniqueKeyValueSetMap, enumNames, enumNameToIntegerValueMap);

    }


    private void extractKeyValues(SequencedMap<Value, VStruct> keyMap, Collection<VStruct> valueList, KeySchema key) {
        int[] keyIndices = FindFieldIndex.findFieldIndices(tableSchema, key);
        for (VStruct value : valueList) {
            Value keyValue = ValueUtil.extractKeyValue(value, keyIndices);
            VStruct old = keyMap.put(keyValue, value);
            if (old != null) {
                errs.addErr(new CfgValueErrs.PrimaryOrUniqueKeyDuplicated(keyValue, tableSchema.name(), key.fields()));
            }
        }
    }

    private static SequencedMap<Value, VStruct> sortIfPKIsNumber(SequencedMap<Value, VStruct> keyMap){
        if (keyMap.size() <= 1) {
            return keyMap;
        }

        Value firstKey = keyMap.firstEntry().getKey();
        if (firstKey instanceof VInt) {
            List<Map.Entry<Value, VStruct>> entryList = new ArrayList<>(keyMap.entrySet());
            entryList.sort(Comparator.comparingInt(e -> ((VInt) e.getKey()).value()));
            SequencedMap<Value, VStruct> sortedMap = new LinkedHashMap<>();
            for (Map.Entry<Value, VStruct> e : entryList) {
                sortedMap.put(e.getKey(), e.getValue());
            }
            return sortedMap;
        } else {
            return keyMap;
        }
    }

}
