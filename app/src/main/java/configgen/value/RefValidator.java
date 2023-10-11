package configgen.value;

import configgen.schema.*;

import static configgen.schema.FieldType.*;
import static configgen.value.CfgValue.*;
import static configgen.value.ValueErrs.*;

public class RefValidator {
    private final CfgValue value;
    private final CfgSchema schema;
    private final ValueErrs errs;

    public RefValidator(CfgValue value, ValueErrs errs) {
        this.value = value;
        this.schema = value.schema();
        this.errs = errs;
    }

    public void validate() {
        presetForeignKeyValueSet();
        for (VTable vTable : value.vTableMap().values()) {
            validateTable(vTable);
        }
    }

    private void presetForeignKeyValueSet() {
        for (Nameable item : schema.items()) {
            switch (item) {
                case InterfaceSchema interfaceSchema -> {
                    for (StructSchema impl : interfaceSchema.impls()) {
                        presetStructural(impl);
                    }
                }
                case Structural structural -> presetStructural(structural);
            }
        }
    }

    private void presetStructural(Structural structural) {
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            switch (fk.refKey()) {
                case RefKey.RefList _ -> {
                }
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
            }
        }
    }

    private void validateTable(VTable vTable) {
        for (VStruct vStruct : vTable.valueList()) {
            validateStruct(vStruct, vTable.schema().name());
        }
    }

    private void validateStruct(VStruct vStruct, String tableName) {
        Structural structural = vStruct.schema();
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            RefKey refKey = fk.refKey();
            if (refKey instanceof RefKey.RefSimple refSimple) {

                FieldType ft = fk.key().obj().get(0).type();
                switch (ft) {
                    case  SimpleType _ -> {
                        Value localValue = ValueUtil.extractKeyValue(vStruct, fk.keyIndices);
                        if (ValueUtil.isValueCellsNotAllEmpty(localValue)) {
                            if (!fk.fkValueSet.contains(localValue)) {
                                errs.addErr(new ForeignValueNotFound(localValue.cells(), tableName, fk.name()));
                            }
                        } else {
                            if (!refSimple.nullable()) {
                                errs.addErr(new RefNotNullableButCellEmpty(localValue.cells(), tableName));
                            }
                        }
                    }
                    case FList _ -> {
                        VList localList = (VList) vStruct.values().get(fk.keyIndices[0]);
                        for (SimpleValue item : localList.valueList()) {
                            if (!fk.fkValueSet.contains(item)) {
                                errs.addErr(new ForeignValueNotFound(item.cells(), tableName, fk.name()));
                            }
                        }
                    }
                    case FMap _ -> {
                        VMap localMap = (VMap) vStruct.values().get(fk.keyIndices[0]);

                        for (SimpleValue val : localMap.valueMap().values()) {
                            if (!fk.fkValueSet.contains(val)) {
                                errs.addErr(new ForeignValueNotFound(val.cells(), tableName, fk.name()));
                            }
                        }
                    }
                }
            }
        }

    }


}
