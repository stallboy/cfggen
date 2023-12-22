package configgen.value;

import configgen.schema.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.value.CfgValue.*;

public class ValueUtil {

    public static Value extractKeyValue(VStruct vStruct, int[] keyIndices) {
        if (keyIndices.length == 0) {
            throw new IllegalArgumentException("key indices empty");
        }
        if (keyIndices.length == 1) {
            return vStruct.values().get(keyIndices[0]);
        }

        List<SimpleValue> values = new ArrayList<>(keyIndices.length);
        for (int keyIndex : keyIndices) {
            values.add((SimpleValue) vStruct.values().get(keyIndex));
        }
        return VList.of(values);
    }

    public static Value extractPrimaryKeyValue(VStruct vStruct, TableSchema tableSchema) {
        int[] keyIndices = FindFieldIndex.findFieldIndices(tableSchema, tableSchema.primaryKey());
        return extractKeyValue(vStruct, keyIndices);
    }

    public static Value extractFieldValue(VStruct vStruct, String fieldName) {
        int idx = FindFieldIndex.findFieldIndex(vStruct.schema(), fieldName);
        if (idx == -1) {
            return null;
        }
        return vStruct.values().get(idx);
    }

    public static Map<Value, VStruct> getForeignKeyValueMap(CfgValue cfgValue, ForeignKeySchema fk) {
        switch (fk.refKey()) {
            case RefKey.RefPrimary ignored -> {
                VTable vTable = cfgValue.vTableMap().get(fk.refTableNormalized());
                return vTable.primaryKeyMap();
            }
            case RefKey.RefUniq refUniq -> {
                VTable vTable = cfgValue.vTableMap().get(fk.refTableNormalized());
                return vTable.uniqueKeyMaps().get(refUniq.keyNames());
            }
            case RefKey.RefList ignored -> {
                return null;
            }
        }
    }

    public static FieldType getKeyFieldType(KeySchema keySchema) {
        List<FieldSchema> keyFields = keySchema.fieldSchemas();
        if (keyFields.size() == 1) {
            return keyFields.getFirst().type();
        }
        StructSchema obj = makeKeyStructSchema(keyFields);
        FieldType.StructRef ref = new FieldType.StructRef("key");
        ref.setObj(obj);
        return ref;
    }

    public static VList vStructToVList(VStruct vStruct) {
        List<SimpleValue> values = new ArrayList<>(vStruct.values().size());
        for (Value value : vStruct.values()) {
            if (value instanceof SimpleValue simpleValue) {
                values.add(simpleValue);
            } else {
                throw new IllegalArgumentException("field value in vStruct not simple");
            }
        }
        return VList.of(values);
    }

    private static StructSchema makeKeyStructSchema(List<FieldSchema> keyFields) {
        return new StructSchema("key", AUTO, Metadata.of(), keyFields, List.of());
    }

    public static boolean isValueCellsNotAllEmpty(Value value) {
        return value.cells().stream().anyMatch(c -> !c.isCellEmpty());
    }

}
