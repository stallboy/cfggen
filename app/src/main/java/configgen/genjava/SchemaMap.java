package configgen.genjava;

import org.jspecify.annotations.Nullable;

public record SchemaMap(Schema key,
                        Schema value) implements Schema {

    @Override
    public boolean compatible(@Nullable Schema other) {
        if (!(other instanceof SchemaMap(Schema key1, Schema value1))) {
            return false;
        }
        return key.compatible(key1) && value.compatible(value1);
    }

    @Override
    public String toString() {
        return "Map<" + key + ", " + value + ">";
    }
}
