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

    public interface FieldableVisitor {
        void visit(Fieldable fieldable);
    }

    /**
     * 展开字段类型里引用的结构并逐个回调：StructRef 本身、FList 的 item、FMap 的 key 和 value（仅当为 StructRef 时）。
     * 回调拿到的 {@link Fieldable} 可能为 null（resolve 完成前 StructRef 尚未挂接 obj），由各调用方按需判空/过滤。
     */
    public static void foreachFieldStructRef(FieldSchema field, FieldableVisitor visitor) {
        switch (field.type()) {
            case FieldType.StructRef structRef -> visitor.visit(structRef.obj());
            case FieldType.FList fList -> {
                if (fList.item() instanceof FieldType.StructRef structRef) {
                    visitor.visit(structRef.obj());
                }
            }
            case FieldType.FMap fMap -> {
                if (fMap.key() instanceof FieldType.StructRef structRef) {
                    visitor.visit(structRef.obj());
                }
                if (fMap.value() instanceof FieldType.StructRef structRef) {
                    visitor.visit(structRef.obj());
                }
            }
            default -> {
            }
        }
    }

}
