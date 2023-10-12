package configgen.schema;

public final class HasBlock {
    public static boolean hasBlock(Nameable nameable) {
        return switch (nameable) {
            case InterfaceSchema sInterface -> sInterface.impls().stream().anyMatch(HasBlock::hasBlock);
            case Structural structural -> structural.fields().stream().anyMatch(HasBlock::hasBlock);
        };
    }

    public static boolean hasBlock(FieldSchema field) {
        return field.fmt() instanceof FieldFormat.Block;
    }

}
