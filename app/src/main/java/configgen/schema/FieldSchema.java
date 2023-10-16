package configgen.schema;

import java.util.Objects;

public record FieldSchema(String name,
                          FieldType type,
                          FieldFormat fmt,
                          Metadata meta) {
    public FieldSchema {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        Objects.requireNonNull(fmt);
        Objects.requireNonNull(meta);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("field name empty");
        }
    }

    /**
     * @return empty when no comment
     */
    public String comment() {
        return meta.getComment();
    }

    public FieldSchema copy() {
        return new FieldSchema(name, type.copy(), fmt, meta.copy());
    }
}
