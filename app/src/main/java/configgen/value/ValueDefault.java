package configgen.value;

import configgen.schema.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.data.CfgData.DCell;
import static configgen.schema.FieldType.Primitive.*;
import static configgen.value.CfgValue.*;

public class ValueDefault {
    private static final VBool DBOOL = new VBool(false, DCell.EMPTY);
    private static final VInt DINT = new VInt(0, DCell.EMPTY);
    private static final VLong DLONG = new VLong(0, DCell.EMPTY);
    private static final VFloat DFLOAT = new VFloat(0, DCell.EMPTY);
    private static final VString DSTR = new VString("", DCell.EMPTY);
    private static final VText DTEXT = new VText("", "", "", DCell.EMPTY);
    private static final VList DLIST = new VList(List.of(), List.of());
    private static final VMap DMAP = new VMap(Map.of(), List.of());

    public static Value of(FieldType type) {
        return switch (type) {
            case BOOL -> DBOOL;
            case INT -> DINT;
            case LONG -> DLONG;
            case FLOAT -> DFLOAT;
            case STRING -> DSTR;
            case TEXT -> DTEXT;
            case FList ignored -> DLIST;
            case FMap ignored -> DMAP;
            case StructRef structRef -> of(structRef.obj());
        };
    }

    public static Value of(Nameable nameable) {
        return switch (nameable) {
            case Structural structural -> ofStructural(structural);
            case InterfaceSchema interfaceSchema -> ofInterface(interfaceSchema);
        };
    }

    public static VStruct ofStructural(Structural structural) {
        List<Value> values = new ArrayList<>(structural.fields().size());
        for (FieldSchema field : structural.fields()) {
            Value fv = of(field.type());
            values.add(fv);
        }
        return new VStruct(structural, values, List.of(DCell.EMPTY));
    }

    public static VInterface ofInterface(InterfaceSchema interfaceSchema) {
        StructSchema impl = interfaceSchema.nullableDefaultImplStruct();
        if (impl == null) {
            impl = interfaceSchema.impls().getFirst();
        }
        VStruct vStruct = ofStructural(impl);
        return new VInterface(interfaceSchema, vStruct, List.of(DCell.EMPTY));
    }
}
