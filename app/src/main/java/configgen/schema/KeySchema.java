package configgen.schema;

import java.util.List;
import java.util.Objects;

public class KeySchema {
    private final List<String> name;
    private List<FieldSchema> obj;

    public KeySchema(List<String> key) {
        Objects.requireNonNull(key);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("keySchema key empty");
        }
        this.name = key;
    }

    public List<String> name() {
        return name;
    }

    public List<FieldSchema> obj() {
        return obj;
    }

    void setObj(List<FieldSchema> obj) {
        this.obj = obj;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeySchema keySchema = (KeySchema) o;
        return Objects.equals(name, keySchema.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "KeySchema{" +
                "name=" + name +
                '}';
    }
}
