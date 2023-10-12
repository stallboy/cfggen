package configgen.schema;

public class ForeachSchema {

    public interface StructuralVisitor {
        void visit(Structural structural, InterfaceSchema nullableFromInterface);
    }

    public static void foreachStructural(StructuralVisitor visitor, CfgSchema cfgSchema) {
        for (Nameable item : cfgSchema.items()) {
            switch (item) {
                case InterfaceSchema interfaceSchema -> {
                    for (StructSchema impl : interfaceSchema.impls()) {
                        visitor.visit(impl, interfaceSchema);
                    }
                }
                case Structural structural -> {
                    visitor.visit(structural, null);
                }
            }
        }
    }

    public interface NameableVisitor {
        void visit(Nameable nameable, InterfaceSchema nullableFromInterface);
    }

    public static void foreachNameable(NameableVisitor visitor, CfgSchema cfgSchema) {
        for (Nameable item : cfgSchema.items()) {
            switch (item) {
                case InterfaceSchema interfaceSchema -> {
                    for (StructSchema impl : interfaceSchema.impls()) {
                        visitor.visit(impl, interfaceSchema);
                    }
                    visitor.visit(interfaceSchema, null);
                }
                case Structural structural -> {
                    visitor.visit(structural, null);
                }
            }
        }
    }

}
