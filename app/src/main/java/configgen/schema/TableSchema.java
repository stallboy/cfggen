package configgen.schema;

import java.util.List;
import java.util.Objects;

import static configgen.schema.FieldFormat.AutoOrPack.*;

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
        return findUniqueKey(ref.fields());
    }

    public KeySchema findUniqueKey(List<String> names) {
        for (KeySchema uk : uniqueKeys) {
            if (uk.fields().equals(names)) {
                return uk;
            }
        }
        return null;
    }

    @Override
    public FieldFormat fmt() {
        return AUTO;
    }
}
