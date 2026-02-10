package configgen.genjava;

public record SchemaList(Schema ele) implements Schema {

    @Override
    public boolean compatible(Schema other) {
        return other instanceof SchemaList(Schema ele1) && ele.compatible(ele1);
    }

    @Override
    public String toString() {
        return "List<" + ele + ">";
    }

}
