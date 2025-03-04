package configgen.value;

import configgen.schema.*;

import java.util.Map;

import static configgen.schema.FieldType.*;
import static configgen.value.CfgValue.*;
import static configgen.value.CfgValueErrs.*;

public class RefValidator {
    private final CfgValue cfgValue;
    private final CfgValueErrs errs;

    public RefValidator(CfgValue value, CfgValueErrs errs) {
        this.cfgValue = value;
        this.errs = errs;
    }

    public void validate() {
        ForeachVStruct.foreach(this::validateVStruct, cfgValue);
    }

    private void validateVStruct(VStruct vStruct, ForeachVStruct.Context ctx) {
        VTable fromTable = ctx.fromVTable();
        Structural structural = vStruct.schema();
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            RefKey refKey = fk.refKey();

            if (refKey instanceof RefKey.RefSimple refSimple) {
                FieldType ft = fk.key().fieldSchemas().getFirst().type();
                switch (ft) {
                    case SimpleType ignored -> {
                        Value localValue = ValueUtil.extractKeyValue(vStruct, fk.keyIndices());
                        if (ValueUtil.isValueFromPackOrSepOrJson(localValue)) {
                            if (refSimple.nullable()) {
                                continue;
                            }
                            Map<Value, VStruct> foreignKeyValueMap = ValueUtil.getForeignKeyValueMap(cfgValue, fk);
                            if (foreignKeyValueMap == null) {
                                continue;
                            }

                            if (!foreignKeyValueMap.containsKey(localValue)) {
                                errs.addErr(new ForeignValueNotFound(localValue, ctx.recordId(), fk.refTable(), fk.name()));
                            }
                        } else {
                            if (ValueUtil.isValueCellsNotAllEmpty(localValue)) {
                                //主键或唯一键，并且nullableRef，--->则可以格子中有值，但ref不到
                                //否则，--->格子中有值，就算配置为nullableRef, 也必须ref到
                                boolean can_NotEmpty_And_NullableRef = structural == fromTable.schema() &&
                                        isForeignLocalKeyInPrimaryOrUniq(fk, fromTable.schema()) && refSimple.nullable();
                                if (can_NotEmpty_And_NullableRef) {
                                    continue;
                                }

                                Map<Value, VStruct> foreignKeyValueMap = ValueUtil.getForeignKeyValueMap(cfgValue, fk);
                                if (foreignKeyValueMap == null) {
                                    continue;
                                }
                                if (!foreignKeyValueMap.containsKey(localValue)) {
                                    errs.addErr(new ForeignValueNotFound(localValue, ctx.recordId(), fk.refTable(), fk.name()));
                                }
                            } else {
                                if (!refSimple.nullable()) {
                                    errs.addErr(new RefNotNullableButCellEmpty(localValue, ctx.recordId()));
                                }
                            }
                        }

                    }
                    case FList ignored -> {
                        Map<Value, VStruct> foreignKeyValueMap = ValueUtil.getForeignKeyValueMap(cfgValue, fk);
                        if (foreignKeyValueMap == null) {
                            continue;
                        }
                        VList localList = (VList) vStruct.values().get(fk.keyIndices()[0]);
                        for (SimpleValue item : localList.valueList()) {
                            if (!foreignKeyValueMap.containsKey(item)) {
                                errs.addErr(new ForeignValueNotFound(item, ctx.recordId(), fk.refTable(), fk.name()));
                            }
                        }
                    }
                    case FMap ignored -> {
                        Map<Value, VStruct> foreignKeyValueMap = ValueUtil.getForeignKeyValueMap(cfgValue, fk);
                        if (foreignKeyValueMap == null) {
                            continue;
                        }

                        VMap localMap = (VMap) vStruct.values().get(fk.keyIndices()[0]);
                        for (SimpleValue val : localMap.valueMap().values()) {
                            if (!foreignKeyValueMap.containsKey(val)) {
                                errs.addErr(new ForeignValueNotFound(val, ctx.recordId(), fk.refTable(), fk.name()));
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isForeignLocalKeyInPrimaryOrUniq(ForeignKeySchema fk, TableSchema table) {
        if (fk.key().fieldSchemas().size() == 1) {
            FieldSchema f = fk.key().fieldSchemas().getFirst();
            for (FieldSchema pkf : table.primaryKey().fieldSchemas()) {
                if (f == pkf) {
                    return true;
                }
            }

            for (KeySchema uk : table.uniqueKeys()) {
                for (FieldSchema ukf : uk.fieldSchemas()) {
                    if (f == ukf) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


}
