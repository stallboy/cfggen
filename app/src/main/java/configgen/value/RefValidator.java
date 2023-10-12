package configgen.value;

import configgen.schema.*;

import static configgen.schema.FieldType.*;
import static configgen.value.CfgValue.*;
import static configgen.value.ValueErrs.*;

public class RefValidator {
    private final CfgValue value;
    private final ValueErrs errs;

    public RefValidator(CfgValue value, ValueErrs errs) {
        this.value = value;
        this.errs = errs;
    }

    public void validate() {
        ForeachStructural.foreach(this::presetStructural, value.schema());
        value.schema().setForeignKeyValueCached();
        ForeachVStruct.foreach(this::validateVStruct, value);
    }

    private void presetStructural(Structural structural, InterfaceSchema nullableFromInterface) {
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            switch (fk.refKey()) {
                case RefKey.RefPrimary _ -> {
                    VTable vTable = value.vTableMap().get(fk.refTableNormalized());
                    fk.fkValueSet = vTable.primaryKeyValueSet();
                    fk.keyIndices = FindFieldIndex.findFieldIndices(structural, fk.key());
                }
                case RefKey.RefUniq refUniq -> {
                    VTable vTable = value.vTableMap().get(fk.refTableNormalized());
                    fk.fkValueSet = vTable.uniqueKeyValueSetMap().get(refUniq.keyNames());
                    fk.keyIndices = FindFieldIndex.findFieldIndices(structural, fk.key());
                }
                case RefKey.RefList _ -> {
                }
            }
        }
    }

    private void validateVStruct(VStruct vStruct, VTable fromTable) {
        Structural structural = vStruct.schema();
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            RefKey refKey = fk.refKey();
            if (refKey instanceof RefKey.RefSimple refSimple) {
                FieldType ft = fk.key().obj().get(0).type();
                switch (ft) {
                    case SimpleType _ -> {
                        Value localValue = ValueUtil.extractKeyValue(vStruct, fk.keyIndices);
                        if (ValueUtil.isValueCellsNotAllEmpty(localValue)) {
                            //主键或唯一键，并且nullableRef，--->则可以格子中有值，但ref不到
                            //否则，--->格子中有值，就算配置为nullableRef也不行
                            boolean canNotEmptyAndNullableRef = structural == fromTable.schema() &&
                                    isForeignLocalKeyInPrimaryOrUniq(fk, fromTable.schema());
                            if (!canNotEmptyAndNullableRef && !fk.fkValueSet.contains(localValue)) {
                                errs.addErr(new ForeignValueNotFound(localValue.cells(), fromTable.name(), fk.name()));
                            }
                        } else {
                            if (!refSimple.nullable()) {
                                errs.addErr(new RefNotNullableButCellEmpty(localValue.cells(), fromTable.name()));
                            }
                        }
                    }
                    case FList _ -> {
                        VList localList = (VList) vStruct.values().get(fk.keyIndices[0]);
                        for (SimpleValue item : localList.valueList()) {
                            if (!fk.fkValueSet.contains(item)) {
                                errs.addErr(new ForeignValueNotFound(item.cells(), fromTable.name(), fk.name()));
                            }
                        }
                    }
                    case FMap _ -> {
                        VMap localMap = (VMap) vStruct.values().get(fk.keyIndices[0]);
                        for (SimpleValue val : localMap.valueMap().values()) {
                            if (!fk.fkValueSet.contains(val)) {
                                errs.addErr(new ForeignValueNotFound(val.cells(), fromTable.name(), fk.name()));
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isForeignLocalKeyInPrimaryOrUniq(ForeignKeySchema fk, TableSchema table) {
        if (fk.key().obj().size() == 1) {
            FieldSchema f = fk.key().obj().get(0);
            for (FieldSchema pkf : table.primaryKey().obj()) {
                if (f == pkf) {
                    return true;
                }
            }

            for (KeySchema uk : table.uniqueKeys()) {
                for (FieldSchema ukf : uk.obj()) {
                    if (f == ukf) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


}
