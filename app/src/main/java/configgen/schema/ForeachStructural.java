package configgen.schema;

public class ForeachStructural {

    public interface StructuralVisitor {
        void visit(Structural structural, InterfaceSchema nullableFromInterface);
    }

    public static void foreach(StructuralVisitor visitor, CfgSchema cfgSchema) {
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
}
