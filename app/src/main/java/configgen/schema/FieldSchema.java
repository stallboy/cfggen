package configgen.schema;


import java.util.Objects;

public record FieldSchema(
        String name,
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
}
