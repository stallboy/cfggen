package configgen.schema;

import java.util.List;
import java.util.Objects;

public record TableSchema(String name,
                          KeySchema primaryKey,
                          EntryType entry,
                          boolean isColumnMode,
                          Metadata meta,

                          List<FieldSchema> fields,
                          List<ForeignKeySchema> foreignKeys,
                          List<KeySchema> uniqueKeys) implements Structural, Nameable {

    public TableSchema {
        Objects.requireNonNull(name);
        Objects.requireNonNull(primaryKey);
        Objects.requireNonNull(entry);
        Objects.requireNonNull(meta);
        Objects.requireNonNull(fields);
        Objects.requireNonNull(foreignKeys);
        Objects.requireNonNull(uniqueKeys);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("table name empty");
        }
    }

    public KeySchema findUniqueKey(KeySchema ref) {
        for (KeySchema uk : uniqueKeys) {
            if (uk.equals(ref)) {
                return uk;
            }
        }
        return null;
    }
}
