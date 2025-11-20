package configgen.schema;


/**
 * 格式为：name = [namespace.]lastName
 */
public sealed interface Nameable permits Fieldable, Structural, StructSchema, InterfaceSchema, TableSchema {

    /**
     * @return 一般 == fullName，包含完整的namespace前缀
     * 只在interface里struct时，name不含namespace前缀
     */
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
    /**
     * @return 一般 == name
     * 在interface里的struct的会再加上interface.name作为namespace前缀
     */
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

    Nameable copy();
}
