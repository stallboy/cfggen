package configgen.genjava;

public final class SchemaRef implements Schema {
    public String type;

    public SchemaRef(ConfigInput input) {
        type = input.readStr();
    }

    public SchemaRef(String type) {
        this.type = type;
    }

    @Override
    public boolean compatible(Schema other) {
        return other instanceof SchemaRef && type.equals(((SchemaRef) other).type);
    }

    @Override
    public void write(ConfigOutput output) {
        output.writeInt(REF);
        output.writeStr(type);
    }

    @Override
    public String toString() {
        return type;
    }

}
