package configgen.schema;

import static configgen.schema.FieldType.*;

public class HasRef {

    public static void preCalculateAllHasRef(CfgSchema schema) {
        ForeachSchema.foreachNameable(HasRef::calcHasRef, schema);
    }

    static final String HAS_REF = "__hasRef";

    private static boolean calcHasRef(Nameable nameable, InterfaceSchema nullableFromInterface) {
        Metadata meta = nameable.meta();
        if (meta.hasTag(HAS_REF)) {
            return true;
        }
        boolean hasRef = switch (nameable) {
            case InterfaceSchema sInterface -> sInterface.impls().stream().anyMatch(im -> calcHasRef(im, sInterface));
            case Structural structural -> !structural.foreignKeys().isEmpty() ||
                    structural.fields().stream().anyMatch(f -> hasRef(f.type()));
        };

        if (hasRef) {
            meta.putTag(HAS_REF);
        }
        return hasRef;
    }

    private static boolean hasRef(FieldType type) {
        switch (type) {
            case Primitive _ -> {
                return false;
            }
            case StructRef structRef -> {
                return calcHasRef(structRef.obj(), null);
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
        return nameable.meta().hasTag(HAS_REF);
    }
}
