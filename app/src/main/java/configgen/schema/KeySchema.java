package configgen.schema;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KeySchema {
    public static boolean isPrimitiveKey(FieldType type) {
        return type == FieldType.Primitive.BOOL || type == FieldType.Primitive.INT
                || type == FieldType.Primitive.LONG || type == FieldType.Primitive.STRING;
    }

    public static boolean isKeyType(FieldType type) {
        return isPrimitiveKey(type) || type instanceof FieldType.StructRef ref
                && ref.obj() instanceof StructSchema struct
                && struct.fields().stream().allMatch(f -> isPrimitiveKey(f.type()));
    }

    private final List<String> fields;
    private @Nullable List<FieldSchema> fieldSchemas;

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
        return java.util.Objects.requireNonNull(fieldSchemas);
    }

    void setFieldSchemas(List<FieldSchema> fieldSchemas) {
        this.fieldSchemas = fieldSchemas;
    }

    @Override
    public boolean equals(@Nullable Object o) {
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
                "fields=" + fields +
                '}';
    }
}
