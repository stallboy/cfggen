package configgen.value;

import configgen.data.CfgData;
import configgen.schema.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.data.Source.*;
import static configgen.value.CfgValue.*;

public class ValueUtil {

    public static VList createList(List<SimpleValue> valueList) {
        if (valueList.isEmpty()) {
            return new VList(valueList, of());
        }
        SimpleValue first = valueList.getFirst();
        if (first.source() instanceof DFile source) {
            return new VList(valueList, source.parent());
        }

        List<CfgData.DCell> list = new ArrayList<>(valueList.size());
        for (SimpleValue v : valueList) {
            switch (v.source()) {
                case CfgData.DCell dCell -> {
                    list.add(dCell);
                }
                case DCellList dCellList -> {
                    list.addAll(dCellList.cells());
                }
                case DFile ignored -> {
                }
            }
        }
        return new VList(valueList, of(list));
    }


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
        return createList(values);
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

    public static String extractFieldValueStr(VStruct vStruct, String fieldName) {
        Value fv = extractFieldValue(vStruct, fieldName);
        if (fv == null) {
            return null;
        }

        if (fv instanceof StringValue stringValue) {
            return stringValue.value();
        } else {
            return fv.packStr();
        }
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

    public static boolean isValueCellsNotAllEmpty(Value value) {
        switch (value.source()) {
            case CfgData.DCell dCell -> {
                return !dCell.isCellEmpty();
            }
            case DCellList dCellList -> {
                return dCellList.cells().stream().anyMatch(c -> !c.isCellEmpty());
            }
            case DFile ignored -> {
                return true;
            }
        }
    }

    public static boolean isValueNumber0(Value value) {
        switch (value) {
            case VInt vInt -> {
                return vInt.value() == 0;
            }
            case VLong vLong -> {
                return vLong.value() == 0;
            }
            case VFloat vFloat -> {
                return vFloat.value() == 0;
            }
            default -> {
                return false;
            }
        }
    }

    public static boolean isValueFromPackOrSepOrJson(Value value) {
        switch (value.source()) {
            case CfgData.DCell dCell -> {
                return dCell.isModePackOrSep();
            }
            case DCellList ignored1 -> {
                return false;
            }
            case DFile ignored -> {
                return true;
            }
        }
    }


}
