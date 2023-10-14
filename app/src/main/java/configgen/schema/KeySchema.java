package configgen.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KeySchema {
    private final List<String> fields;
    private List<FieldSchema> fieldSchemas;

    public KeySchema(List<String> key) {
        Objects.requireNonNull(key);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("keySchema key empty");
        }
        this.fields = key;
    }

    public KeySchema copy() {
        return new KeySchema(new ArrayList<>(fields));
    }

    public List<String> fields() {
        return fields;
    }

    public List<FieldSchema> fieldSchemas() {
        return fieldSchemas;
    }

    void setFieldSchemas(List<FieldSchema> fieldSchemas) {
        this.fieldSchemas = fieldSchemas;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeySchema keySchema = (KeySchema) o;
        return Objects.equals(fields, keySchema.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields);
    }

    @Override
    public String toString() {
        return "KeySchema{" +
                "name=" + fields +
                '}';
    }
}
