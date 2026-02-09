package configgen.genjava;

public enum SchemaPrimitive implements Schema {
    SBool, SInt, SLong, SFloat, SStr, SText;

    @Override
    public boolean compatible(Schema other) {
        return this == other;
    }
}
