package configgen.schema;

public class ForeachSchema {

    public interface StructuralVisitor {
        void visit(Structural structural);
    }

    public static void foreachStructural(StructuralVisitor visitor, CfgSchema cfgSchema) {
        for (Nameable item : cfgSchema.items()) {
            switch (item) {
                case InterfaceSchema interfaceSchema -> {
                    for (StructSchema impl : interfaceSchema.impls()) {
                        visitor.visit(impl);
                    }
                }
                case Structural structural -> {
                    visitor.visit(structural);
                }
            }
        }
    }

    public interface NameableVisitor {
        void visit(Nameable nameable);
    }

    public static void foreachNameable(NameableVisitor visitor, CfgSchema cfgSchema) {
        for (Nameable item : cfgSchema.items()) {
            switch (item) {
                case InterfaceSchema interfaceSchema -> {
                    for (StructSchema impl : interfaceSchema.impls()) {
                        visitor.visit(impl);
                    }
                    visitor.visit(interfaceSchema);
                }
                case Structural structural -> {
                    visitor.visit(structural);
                }
            }
        }
    }

}
