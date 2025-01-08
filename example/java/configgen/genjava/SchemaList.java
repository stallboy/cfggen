package configgen.genjava;

public final class SchemaList implements Schema {
    public final Schema ele;

    public SchemaList(ConfigInput input) {
        ele = Schema.create(input);
    }

    public SchemaList(Schema ele) {
        this.ele = ele;
    }

    @Override
    public boolean compatible(Schema other) {
        return other instanceof SchemaList os && ele.compatible(os.ele);
    }

    @Override
    public void write(ConfigOutput output) {
        output.writeInt(LIST);
        ele.write(output);
    }

    @Override
    public String toString() {
        return "List<" + ele + ">";
    }

}
