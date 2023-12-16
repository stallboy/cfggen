package configgen.schema;

import java.util.Objects;

public class ForeignKeySchema {
    private final String name;
    private final KeySchema key;
    private final String refTable;
    private final RefKey refKey;
    private final Metadata meta;

    private TableSchema refTableSchema;
    private int[] keyIndices;

    public ForeignKeySchema(String name, KeySchema key, String refTable, RefKey refKey, Metadata meta) {
        this.name = name;
        this.key = key;
        this.refTable = refTable;
        this.refKey = refKey;
        this.meta = meta;
        Objects.requireNonNull(name);
        Objects.requireNonNull(key);
        Objects.requireNonNull(refTable);
        Objects.requireNonNull(refKey);
        Objects.requireNonNull(meta);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("struct name empty");
        }
    }

    public ForeignKeySchema copy() {
        return new ForeignKeySchema(name, key.copy(), refTable, refKey.copy(), meta.copy());
    }

    public String name() {
        return name;
    }

    public KeySchema key() {
        return key;
    }

    public String refTable() {
        return refTable;
    }

    public String refTableNormalized() {
        return refTableSchema.name();
    }

    public RefKey refKey() {
        return refKey;
    }

    public TableSchema refTableSchema() {
        return refTableSchema;
    }

    public int[] keyIndices() {
        return keyIndices;
    }

    void setRefTableSchema(TableSchema refTableSchema) {
        this.refTableSchema = refTableSchema;
    }

    public void setKeyIndices(int[] keyIndices) {
        this.keyIndices = keyIndices;
    }


    public Metadata meta() {
        return meta;
    }

    @Override
    public String toString() {
        return "ForeignKeySchema{" +
                "name='" + name + '\'' +
                ", key=" + key +
                ", refTable='" + refTable + '\'' +
                ", refKey=" + refKey +
                ", meta=" + meta +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForeignKeySchema that = (ForeignKeySchema) o;
        return Objects.equals(name, that.name) && Objects.equals(key, that.key) && Objects.equals(refTable, that.refTable) && Objects.equals(refKey, that.refKey) && Objects.equals(meta, that.meta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, key, refTable, refKey, meta);
    }


}
