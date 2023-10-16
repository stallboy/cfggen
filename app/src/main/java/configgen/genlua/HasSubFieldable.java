package configgen.genlua;

import configgen.schema.FieldSchema;
import configgen.schema.Structural;

import static configgen.schema.FieldType.*;

class HasSubFieldable {
    static boolean hasSubFieldable(Structural structural) {
        for (FieldSchema field : structural.fields()) {
            switch (field.type()) {
                case StructRef _ -> {
                    return true;
                }
                case FList fList -> {
                    if (fList.item() instanceof StructRef) {
                        return true;
                    }
                }
                case FMap fMap -> {
                    if (fMap.key() instanceof StructRef || fMap.value() instanceof StructRef) {
                        return true;
                    }
                }
                case Primitive _ -> {
                }
            }
        }
        return false;
    }
}
