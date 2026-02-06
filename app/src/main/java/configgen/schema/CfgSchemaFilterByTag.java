package configgen.schema;

import java.util.*;

import static configgen.schema.EntryType.*;
import static configgen.schema.CfgSchemaErrs.FilterRefIgnoredByRefKeyNotFound;
import static configgen.schema.CfgSchemaErrs.FilterRefIgnoredByRefTableNotFound;

/**
 * tag： 只标注field的就行，不用标注foreign key，
 * foreign key是否提取，只由是否可行决定，能包含就包含。
 * <p>
 * 如果在structural上配置了tag，分3种情况
 * <ol>1，所有field都没tag、-tag, 则包含所有field</ol>
 * <ol>2，有部分field设了tag，则取这设置了tag的field</ol>
 * <ol>3，没有设置tag的，但有部分设置了-tag，则提取没设-tag的field</ol>
 * <p>
 * 一般情况下，impl不需要设置tag，
 * 如果impl上设置tag，则则是为了能filter出空结构，相当于只用此impl类名字做标志，
 * 普通的struct不支持filter出空结构。
 */
public class CfgSchemaFilterByTag {
    private final CfgSchema cfg;
    private final String tag;
    private final boolean isMinusTag;
    private final String minusTag;
    private final String noMinusTag;
    private final CfgSchemaErrs errs;


    public CfgSchemaFilterByTag(CfgSchema cfg, String tag, CfgSchemaErrs errs) {
        cfg.requireResolved();
        Objects.requireNonNull(tag);
        if (tag.isEmpty()) {
            throw new IllegalArgumentException("filter tag empty");
        }
        Objects.requireNonNull(errs);
        this.cfg = cfg;
        this.tag = tag;
        this.isMinusTag = tag.startsWith("-");
        this.minusTag = "-" + tag;
        this.noMinusTag = isMinusTag ? tag.substring(1) : tag;
        this.errs = errs;
    }

    public CfgSchema filter() {
        Map<String, TableSchema> tableMap = buildFilteredTableMap();

        CfgSchema filtered = CfgSchema.ofPartial();
        for (Nameable item : cfg.items()) {
            switch (item) {
                case InterfaceSchema interfaceSchema -> {
                    if (hasTagForInterface(interfaceSchema)) {
                        filtered.add(filterInterface(interfaceSchema, tableMap));
                    }
                }
                case StructSchema structSchema -> {
                    if (hasTagForStructural(structSchema)) {
                        filtered.add(filterStruct(structSchema, false, tableMap));
                    }
                }
                case TableSchema tableSchema -> {
                    TableSchema ts = tableMap.get(tableSchema.name());
                    if (ts != null) {
                        filtered.items().add(tablePhase2_handleForeignKey(tableSchema, ts, tableMap));
                    }
                }
            }
        }
        return filtered;
    }

    private Map<String, TableSchema> buildFilteredTableMap() {
        Map<String, TableSchema> tableMap = new HashMap<>();
        for (Nameable item : cfg.items()) {
            if (item instanceof TableSchema tableSchema && hasTagForStructural(tableSchema)) {
                TableSchema ts = tablePhase1_filter(tableSchema);
                tableMap.put(ts.name(), ts);
            }
        }
        return tableMap;
    }

    /////////////////////////////// filter by tag 规则
    private boolean hasTagForInterface(InterfaceSchema interfaceSchema) {
        if (isMinusTag) {
            return !interfaceSchema.meta().hasTag(noMinusTag);
        } else {
            return interfaceSchema.meta().hasTag(tag);
        }
    }

    private boolean hasTagForStructural(Structural struct) {
        if (isMinusTag) {
            return !struct.meta().hasTag(noMinusTag);
        } else {
            if (struct.meta().hasTag(tag)) {
                return true;
            }
            for (FieldSchema field : struct.fields()) {
                if (field.meta().hasTag(tag)) {
                    return true;
                }
            }
            return false;
        }
    }

    private List<FieldSchema> filterFields(Structural structural, boolean isImpl) {
        if (isMinusTag) {
            List<FieldSchema> filteredFields = new ArrayList<>();
            for (FieldSchema field : structural.fields()) {
                if (!field.meta().hasTag(noMinusTag)) {
                    filteredFields.add(field.copy());
                }
            }
            return filteredFields;

        } else {
            List<FieldSchema> withTagFields = new ArrayList<>();
            int withMinusTagFieldCount = 0;
            for (FieldSchema field : structural.fields()) {
                if (field.meta().hasTag(tag)) {
                    withTagFields.add(field.copy());
                } else if (field.meta().hasTag(minusTag)) {
                    withMinusTagFieldCount++;
                }
            }

            @SuppressWarnings("UnnecessaryLocalVariable")
            List<FieldSchema> filteredFields = withTagFields;
            if (withTagFields.isEmpty()) {
                //noinspection StatementWithEmptyBody
                if (isImpl && structural.meta().hasTag(tag)) {
                    //一般情况下，impl不需要设置tag，
                    // 如果impl上设置tag，则则是为了能filter出空结构，相当于只用此impl类名字做标志，
                } else if (withMinusTagFieldCount > 0) {
                    for (FieldSchema field : structural.fields()) {
                        if (!field.meta().hasTag(minusTag)) {
                            filteredFields.add(field.copy());
                        }
                    }
                } else {
                    for (FieldSchema field : structural.fields()) {
                        filteredFields.add(field.copy());
                    }
                }
            }
            return filteredFields;
        }
    }
    ///////////////////////////////

    private StructSchema filterStruct(StructSchema struct, boolean isImpl, Map<String, TableSchema> tableMap) {
        List<FieldSchema> filteredFields = filterFields(struct, isImpl);
        List<ForeignKeySchema> fks = filterForeignKeys(struct, filteredFields, tableMap);
        return new StructSchema(struct.name(), struct.fmt(), struct.meta().copyWithoutState(), filteredFields, fks);
    }

    private InterfaceSchema filterInterface(InterfaceSchema sInterface, Map<String, TableSchema> tableMap) {
        List<StructSchema> impls = new ArrayList<>(sInterface.impls().size());
        for (StructSchema impl : sInterface.impls()) {
            impls.add(filterStruct(impl, true, tableMap));
        }
        return new InterfaceSchema(sInterface.name(), sInterface.enumRef(), sInterface.defaultImpl(),
                sInterface.fmt(), sInterface.meta().copyWithoutState(), impls);
    }


    private static boolean isFieldIn(String name, List<FieldSchema> filteredFields) {
        return filteredFields.stream().anyMatch(f -> f.name().equals(name));
    }

    private static boolean isKeyIn(KeySchema key, List<FieldSchema> filteredFields) {
        return key.fields().stream().allMatch(name -> isFieldIn(name, filteredFields));
    }


    /**
     * 阶段1，外键未处理，其他都处理了
     */
    private TableSchema tablePhase1_filter(TableSchema table) {
        List<FieldSchema> filteredFields = filterFields(table, false);
        EntryType entry = ENo.NO;
        switch (table.entry()) {
            case ENo.NO -> {
            }
            case EEntry ee -> {
                if (isFieldIn(ee.field, filteredFields)) {
                    entry = new EEntry(ee.field);
                }
            }
            case EEnum ee -> {
                if (isFieldIn(ee.field, filteredFields)) {
                    entry = new EEnum(ee.field);
                }
            }
        }
        List<KeySchema> uks = filterUniqKeys(table, filteredFields);
        return new TableSchema(table.name(), table.primaryKey().copy(), entry, table.isColumnMode(),
                table.meta().copyWithoutState(), filteredFields, List.of(), uks);
    }

    /**
     * 阶段2，处理外键
     */
    private TableSchema tablePhase2_handleForeignKey(TableSchema originalTable,
                                                     TableSchema table,
                                                     Map<String, TableSchema> phase1TableMap) {
        List<ForeignKeySchema> fks = filterForeignKeys(originalTable, table.fields(), phase1TableMap);

        return new TableSchema(table.name(), table.primaryKey(), table.entry(), table.isColumnMode(),
                table.meta().copyWithoutState(), table.fields(), fks, table.uniqueKeys());

    }


    private List<ForeignKeySchema> filterForeignKeys(Structural originalStructural,
                                                     List<FieldSchema> filteredFields,
                                                     Map<String, TableSchema> phase1FilteredTableMap) {
        List<ForeignKeySchema> resultFks = new ArrayList<>();
        for (ForeignKeySchema fk : originalStructural.foreignKeys()) {
            if (isKeyIn(fk.key(), filteredFields)) {
                recordForeignKeyIfOk(resultFks, fk, originalStructural, phase1FilteredTableMap);
            }
        }
        return resultFks;
    }

    private void recordForeignKeyIfOk(List<ForeignKeySchema> resultFks, ForeignKeySchema fk,
                                      Structural structural, Map<String, TableSchema> phase1TableMap) {
        RefErr err = isForeignKeyIn(structural, fk, phase1TableMap);
        switch (err) {
            case OK -> resultFks.add(fk.copy());
            case TABLE_NOT_FOUND -> errs.addWeakWarn(new FilterRefIgnoredByRefTableNotFound(
                    structural.name(), fk.name(), fk.refTable()));
            case KEY_NOT_FOUND -> errs.addWeakWarn(new FilterRefIgnoredByRefKeyNotFound(
                    structural.name(), fk.name(), fk.refTable(), fk.refKey().keyNames()));
        }
    }

    private enum RefErr {
        OK,
        TABLE_NOT_FOUND,
        KEY_NOT_FOUND
    }

    private RefErr isForeignKeyIn(Structural structural, ForeignKeySchema fk, Map<String, TableSchema> phase1TableMap) {
        TableSchema refTable = null;
        // 本模块找
        String namespace = structural.namespace();
        if (!namespace.isEmpty()) {
            String fullName = Nameable.makeName(namespace, fk.refTable());
            refTable = phase1TableMap.get(fullName);
        }

        // 全局找
        if (refTable == null) {
            refTable = phase1TableMap.get(fk.refTable());
        }

        if (refTable == null) {
            return RefErr.TABLE_NOT_FOUND;
        }

        switch (fk.refKey()) {
            case RefKey.RefPrimary ignored -> {
                return RefErr.OK;
            }
            case RefKey.RefUniq refUniq -> {
                KeySchema uk = refTable.findUniqueKey(refUniq.key());
                if (uk != null) {
                    return RefErr.OK;
                } else {
                    return RefErr.KEY_NOT_FOUND;
                }
            }
            case RefKey.RefList refList -> {
                Set<String> names = new HashSet<>();
                for (FieldSchema field : refTable.fields()) {
                    names.add(field.name());
                }
                if (names.containsAll(refList.key().fields())) {
                    return RefErr.OK;
                } else {
                    return RefErr.KEY_NOT_FOUND;
                }
            }
        }
    }

    private List<KeySchema> filterUniqKeys(TableSchema table, List<FieldSchema> ff) {
        List<KeySchema> uks = new ArrayList<>();
        for (KeySchema uk : table.uniqueKeys()) {
            if (isKeyIn(uk, ff)) {
                uks.add(uk.copy());
            }
        }
        return uks;
    }

}
