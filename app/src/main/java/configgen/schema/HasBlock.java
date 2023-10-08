package configgen.schema;

public final class HasBlock {
    public static boolean hasBlock(Nameable nameable) {
        switch (nameable) {
            case InterfaceSchema interfaceSchema -> {
                for (StructSchema impl : interfaceSchema.impls()) {
                    if (hasBlock(impl)) {
                        return true;
                    }
                }

            }
            case Structural structural -> {
                for (FieldSchema field : structural.fields()) {
                    if (hasBlock(field)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasBlock(FieldSchema field) {
        return field.fmt() instanceof FieldFormat.Block;
    }
}
