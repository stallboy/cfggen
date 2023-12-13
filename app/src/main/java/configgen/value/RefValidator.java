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
        ForeachSchema.foreachStructural(this::presetStructural, value.schema());
        value.schema().setForeignKeyValueCached();
        ForeachVStruct.foreach(this::validateVStruct, value);
    }

    private void presetStructural(Structural structural) {
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            fk.keyIndices = FindFieldIndex.findFieldIndices(structural, fk.key());

            switch (fk.refKey()) {
                case RefKey.RefPrimary ignored -> {
                    VTable vTable = value.vTableMap().get(fk.refTableNormalized());
                    fk.fkValueMap = vTable.primaryKeyMap();
                }
                case RefKey.RefUniq refUniq -> {
                    VTable vTable = value.vTableMap().get(fk.refTableNormalized());
                    fk.fkValueMap = vTable.uniqueKeyMaps().get(refUniq.keyNames());
                }
                case RefKey.RefList ignored -> {
                }
            }
        }
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
                        Value localValue = ValueUtil.extractKeyValue(vStruct, fk.keyIndices);
                        if (ValueUtil.isValueCellsNotAllEmpty(localValue)) {
                            //主键或唯一键，并且nullableRef，--->则可以格子中有值，但ref不到
                            //否则，--->格子中有值，就算配置为nullableRef也不行
                            boolean can_NotEmpty_And_NullableRef = structural == fromTable.schema() &&
                                    isForeignLocalKeyInPrimaryOrUniq(fk, fromTable.schema()) && refSimple.nullable();
                            if (!can_NotEmpty_And_NullableRef && !fk.fkValueMap.containsKey(localValue)) {
                                errs.addErr(new ForeignValueNotFound(localValue.cells(), fromTable.name(), fk.name()));
                            }
                        } else {
                            if (!refSimple.nullable()) {
                                errs.addErr(new RefNotNullableButCellEmpty(localValue.cells(), fromTable.name()));
                            }
                        }
                    }
                    case FList ignored -> {
                        VList localList = (VList) vStruct.values().get(fk.keyIndices[0]);
                        for (SimpleValue item : localList.valueList()) {
                            if (!fk.fkValueMap.containsKey(item)) {
                                errs.addErr(new ForeignValueNotFound(item.cells(), fromTable.name(), fk.name()));
                            }
                        }
                    }
                    case FMap ignored -> {
                        VMap localMap = (VMap) vStruct.values().get(fk.keyIndices[0]);
                        for (SimpleValue val : localMap.valueMap().values()) {
                            if (!fk.fkValueMap.containsKey(val)) {
                                errs.addErr(new ForeignValueNotFound(val.cells(), fromTable.name(), fk.name()));
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
