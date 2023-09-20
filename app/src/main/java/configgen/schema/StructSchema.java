package configgen.schema;

import java.util.List;
import java.util.Objects;

public record StructSchema(String name,
                           FieldFormat fmt,
                           Metadata meta,
                           List<FieldSchema> fields,
                           List<ForeignKeySchema> foreignKeys) implements Fieldable, Structural, Nameable {
    public StructSchema {
        Objects.requireNonNull(name);
        Objects.requireNonNull(fmt);
        Objects.requireNonNull(meta);
        Objects.requireNonNull(fields);
        Objects.requireNonNull(foreignKeys);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("struct name empty");
        }
    }
}
