package configgen.schema;

public class ForeachStructural {

    public interface StructuralVisitor {
        void accept(Structural structural, InterfaceSchema nullableFromInterface);
    }

    public static void foreach(StructuralVisitor visitor, CfgSchema cfgSchema) {
        for (Nameable item : cfgSchema.items()) {
            switch (item) {
                case InterfaceSchema interfaceSchema -> {
                    for (StructSchema impl : interfaceSchema.impls()) {
                        visitor.accept(impl, interfaceSchema);
                    }
                }
                case Structural structural -> {
                    visitor.accept(structural, null);
                }
            }
        }
    }
}
