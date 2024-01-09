package configgen.schema;


/**
 * 格式为：name = [namespace.]lastName
 */
public sealed interface Nameable permits Fieldable, Structural, StructSchema, InterfaceSchema, TableSchema {
    String name();

    FieldFormat fmt();

    Metadata meta();

    default String comment() {
        return meta().getComment();
    }

    default String namespace() {
        int idx = name().lastIndexOf('.');
        if (idx == -1) {
            return "";
        }
        return name().substring(0, idx);
    }

    default String lastName() {
        int idx = name().lastIndexOf('.');
        if (idx == -1) {
            return name();
        }
        return name().substring(idx + 1);
    }

    default String fullName() {
        return name();
    }

    default String stringify() {
        CfgSchema schema = CfgSchema.of();
        schema.add(this);
        return schema.stringify();
    }

    static String makeName(String namespace, String lastName) {
        return namespace + "." + lastName;
    }
}
