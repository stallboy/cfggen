package configgen.genjava;

import org.jetbrains.annotations.NotNull;

public record SchemaMap(Schema key,
                        Schema value) implements Schema {

    @Override
    public boolean compatible(Schema other) {
        if (!(other instanceof SchemaMap(Schema key1, Schema value1))) {
            return false;
        }
        return key.compatible(key1) && value.compatible(value1);
    }

    @Override
    public @NotNull String toString() {
        return "Map<" + key + ", " + value + ">";
    }
}
