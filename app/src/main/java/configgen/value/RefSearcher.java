package configgen.value;

import configgen.schema.*;

import java.util.*;

import static configgen.schema.RefKey.*;
import static configgen.value.CfgValue.*;
import static configgen.value.RefSearcher.RefSearchErr.*;

public final class RefSearcher {

    public enum RefSearchErr {
        Ok,
        TableNotFound,
        UniqueKeyNotFound,
    }

    public record RefSearchResult(RefSearchErr err,
                                  Map<Value, Set<String>> value2tables) {

    }


    /**
     * 搜索对一个table主键或唯一键的引用
     *
     * @return value : ref到这个value的table列表。（当refTable找不到，或uniqueKey找不到时，返回null)
     * 如果nullableUniqueKeys == null： value是refTable的主键具体值，
     * 否则：value是唯一键的具体值
     */
    public static RefSearchResult search(CfgValue cfgValue, String refTableName,
                                         List<String> nullableUniqueKeys, Set<String> ignoredTables) {
        cfgValue.schema().requireForeignKeyValueCached();
        TableSchema refTable = cfgValue.schema().findTable(refTableName);
        if (refTable == null) {
            return new RefSearchResult(TableNotFound, null);
        }

        if (nullableUniqueKeys != null && refTable.findUniqueKey(nullableUniqueKeys) == null) {
            return new RefSearchResult(UniqueKeyNotFound, null);
        }

        Map<Value, Set<String>> res = new LinkedHashMap<>();
        ForeachVStruct.VStructVisitor vStructVisitor = (vStruct, fromVTable) -> search(vStruct, fromVTable, refTable, nullableUniqueKeys, res);
        for (VTable vTable : cfgValue.tables()) {
            if (!ignoredTables.contains(vTable.name())) {
                ForeachVStruct.foreachVTable(vStructVisitor, vTable);
            }
        }
        return new RefSearchResult(Ok, res);
    }

    private static void search(VStruct vStruct, VTable fromTable, TableSchema refTable, List<String> nullableUniqueKeys, Map<Value, Set<String>> res) {
        Structural structural = vStruct.schema();
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            RefKey refKey = fk.refKey();
            if (fk.refTableSchema() != refTable) {
                continue;
            }

            boolean match = false;
            if (nullableUniqueKeys == null) {
                match = refKey instanceof RefPrimary;
            } else if (refKey instanceof RefUniq uniq) {
                match = uniq.keyNames().equals(nullableUniqueKeys);
            }

            if (match) { // 只搜索主键索引
                FieldType ft = fk.key().obj().get(0).type();
                switch (ft) {
                    case FieldType.SimpleType _ -> {
                        Value localValue = ValueUtil.extractKeyValue(vStruct, fk.keyIndices);
                        if (fk.fkValueSet.contains(localValue)) {
                            addValueTable(res, localValue, fromTable.name());
                        }
                    }
                    case FieldType.FList _ -> {
                        VList localList = (VList) vStruct.values().get(fk.keyIndices[0]);
                        for (SimpleValue item : localList.valueList()) {
                            addValueTable(res, item, fromTable.name());

                        }
                    }
                    case FieldType.FMap _ -> {
                        VMap localMap = (VMap) vStruct.values().get(fk.keyIndices[0]);
                        for (SimpleValue val : localMap.valueMap().values()) {
                            addValueTable(res, val, fromTable.name());
                        }
                    }
                }
            }
        }
    }

    private static void addValueTable(Map<Value, Set<String>> res, Value value, String tableName) {
        Set<String> tables = res.computeIfAbsent(value, k -> new LinkedHashSet<>());
        tables.add(tableName);
    }

}
