package configgen.schema;

public sealed interface Fieldable extends Nameable permits StructSchema, InterfaceSchema {
    FieldFormat fmt();
}
