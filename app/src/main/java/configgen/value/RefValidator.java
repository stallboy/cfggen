package configgen.value;

import configgen.schema.*;

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
        preSetForeignKeyValueSet();
        for (CfgValue.VTable vTable : value.vTableMap().values()) {
            validateTable(vTable);
        }

    }

    private void preSetForeignKeyValueSet() {
        for (Nameable item : schema.items()) {
            switch (item) {
                case InterfaceSchema interfaceSchema -> {
                    for (StructSchema impl : interfaceSchema.impls()) {
                        preSetStructuralFk(impl);
                    }
                }
                case Structural structural -> {
                    preSetStructuralFk(structural);
                }
            }
        }
    }

    private void preSetStructuralFk(Structural structural) {
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            switch (fk.refKey()) {
                case RefKey.RefList _ -> {
                }
                case RefKey.RefPrimary _ -> {
                    CfgValue.VTable vTable = value.vTableMap().get(fk.refTable());
                    fk.fkValueSet = vTable.primaryKeyValueSet();
                    fk.keyIndices = FindFieldIndex.findFieldIndices(structural, fk.key());
                }
                case RefKey.RefUniq refUniq -> {
                    CfgValue.VTable vTable = value.vTableMap().get(fk.refTable());
                    fk.fkValueSet = vTable.uniqueKeyValueSetMap().get(refUniq.keyNames());
                    fk.keyIndices = FindFieldIndex.findFieldIndices(structural, fk.key());
                }
            }
        }
    }

    private void validateTable(CfgValue.VTable vTable) {
        for (CfgValue.VStruct vStruct : vTable.valueList()) {
            validateStruct(vStruct);
        }

    }

    private void validateStruct(CfgValue.VStruct vStruct) {
        Structural structural = vStruct.schema();
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            RefKey refKey = fk.refKey();
            if (refKey instanceof RefKey.RefSimple){
                CfgValue.Value localValue = ValueUtil.extract(vStruct, fk.keyIndices);
                if (!fk.fkValueSet.contains(localValue)){

                }

            }

        }

    }


}
