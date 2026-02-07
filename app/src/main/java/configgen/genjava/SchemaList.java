package configgen.genjava;

import org.jetbrains.annotations.NotNull;

public record SchemaList(Schema ele) implements Schema {

    @Override
    public boolean compatible(Schema other) {
        return other instanceof SchemaList(Schema ele1) && ele.compatible(ele1);
    }

    @Override
    public @NotNull String toString() {
        return "List<" + ele + ">";
    }

}
