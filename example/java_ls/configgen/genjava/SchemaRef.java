package configgen.genjava;

public final class SchemaRef implements Schema {
    public String type;

    public SchemaRef(String type) {
        this.type = type;
    }

    @Override
    public boolean compatible(Schema other) {
        return other instanceof SchemaRef && type.equals(((SchemaRef) other).type);
    }

    @Override
    public String toString() {
        return type;
    }

}
