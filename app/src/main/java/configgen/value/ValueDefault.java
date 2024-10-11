package configgen.value;

import configgen.data.Source;
import configgen.schema.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.schema.FieldType.Primitive.*;
import static configgen.value.CfgValue.*;

public class ValueDefault {

    public static Value of(FieldType type, Source source) {
        return switch (type) {
            case BOOL -> new VBool(false, source);
            case INT -> new VInt(0, source);
            case LONG -> new VLong(0, source);
            case FLOAT -> new VFloat(0, source);
            case STRING -> new VString("", source);
            case TEXT -> new VText("", "", null, source);
            case FList ignored -> new VList(List.of(), source);
            case FMap ignored -> new VMap(Map.of(), source);
            case StructRef structRef -> ofNamable(structRef.obj(), source);
        };
    }


    public static boolean isDefault(Value value) {
        return switch (value) {
            case VBool vBool -> !vBool.value();
            case VInt vInt -> vInt.value() == 0;
            case VLong vLong -> vLong.value() == 0;
            case VFloat vFloat -> vFloat.value() == 0;
            case VString vStr -> vStr.value().isEmpty();
            case VText vText -> vText.value().isEmpty();
            case VStruct ignored -> false;
            case VInterface ignored -> false;
            case VList vList -> vList.valueList().isEmpty();
            case VMap vMap -> vMap.valueMap().isEmpty();
        };
    }

    public static Value ofNamable(Nameable nameable, Source source) {
        return switch (nameable) {
            case Structural structural -> ofStructural(structural, source);
            case InterfaceSchema interfaceSchema -> ofInterface(interfaceSchema, source);
        };
    }

    public static VStruct ofStructural(Structural structural, Source source) {
        List<Value> values = new ArrayList<>(structural.fields().size());
        for (FieldSchema field : structural.fields()) {
            Value fv = of(field.type(), source);
            values.add(fv);
        }
        return new VStruct(structural, values, source);
    }

    public static VInterface ofInterface(InterfaceSchema interfaceSchema, Source source) {
        StructSchema impl = interfaceSchema.nullableDefaultImplStruct();
        if (impl == null) {
            impl = interfaceSchema.impls().getFirst();
        }
        VStruct vStruct = ofStructural(impl, source);
        return new VInterface(interfaceSchema, vStruct, source);
    }
}
