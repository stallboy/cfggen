package configgen.schema;

import static configgen.schema.FieldType.*;

public class HasRef {

    public static void preCalculateAllHasRef(CfgSchema schema) {
        ForeachSchema.foreachNameable(HasRef::calcHasRef, schema);
    }

    private static boolean calcHasRef(Nameable nameable) {
        Metadata meta = nameable.meta();
        if (meta.getHasRef()) {
            return true;
        }
        boolean hasRef = switch (nameable) {
            case InterfaceSchema sInterface -> sInterface.impls().stream().anyMatch(HasRef::calcHasRef);
            case Structural structural -> !structural.foreignKeys().isEmpty() ||
                    structural.fields().stream().anyMatch(f -> hasRef(f.type()));
        };

        if (hasRef) {
            meta.putHasRef();
        }
        return hasRef;
    }

    public static boolean hasRef(FieldType type) {
        switch (type) {
            case Primitive _ -> {
                return false;
            }
            case StructRef structRef -> {
                return calcHasRef(structRef.obj());
            }
            case FList fList -> {
                return hasRef(fList.item());
            }

            case FMap fMap -> {
                return hasRef(fMap.key()) || hasRef(fMap.value());
            }
        }
    }

    public static boolean hasRef(Nameable nameable) {
        return nameable.meta().getHasRef();
    }
}
