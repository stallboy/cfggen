package configgen.value;

import configgen.ctx.TextI18n;
import configgen.data.CfgData;
import configgen.data.Source;
import configgen.schema.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;

public class ValueUtil {


    public static VText createText(String str, Source source, TextI18n.TableI18n nullableTableI18n) {
        String i18n = null;
        String value;
        if (nullableTableI18n != null) {
            i18n = nullableTableI18n.findText(str);
            value = i18n != null ? i18n : str;
        } else {
            value = str;
        }
        return new VText(value, str, i18n, source);
    }

    public static VList createList(List<SimpleValue> valueList) {
        if (valueList.isEmpty()) {
            return new VList(valueList, Source.of());
        }
        SimpleValue first = valueList.getFirst();
        if (first.source() instanceof Source.DFile) {
            return new VList(valueList, first.source());
        }

        List<CfgData.DCell> list = new ArrayList<>(valueList.size());
        for (SimpleValue v : valueList) {
            switch (v.source()) {
                case CfgData.DCell dCell -> {
                    list.add(dCell);
                }
                case Source.DCellList dCellList -> {
                    list.addAll(dCellList.cells());
                }
                case Source.DFile ignored -> {
                }
            }
        }
        return new VList(valueList, Source.of(list));
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
            case Source.DCellList dCellList -> {
                return dCellList.cells().stream().anyMatch(c -> !c.isCellEmpty());
            }
            case Source.DFile ignored -> {
                return true;
            }
        }
    }

}
