package configgen.genjava;

import org.jspecify.annotations.Nullable;

public final class SchemaRef implements Schema {
    public String type;

    public SchemaRef(String type) {
        this.type = type;
    }

    @Override
    public boolean compatible(@Nullable Schema other) {
        return other instanceof SchemaRef ref && type.equals(ref.type);
    }

    @Override
    public String toString() {
        return type;
    }

}
