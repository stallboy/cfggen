package configgen.genjava;

public sealed interface Schema permits SchemaBean, SchemaEnum, SchemaInterface, SchemaList, SchemaMap, SchemaPrimitive, SchemaRef {
    /**
     * 比较兼容性，codeSchema.compatible(dataSchema);
     * 参照example/javaload/LoadConfig.java
     */
    boolean compatible(Schema other);
    void write(ConfigOutput output);

    int BOOL = 1;
    int INT = 2;
    int LONG = 3;
    int FLOAT = 4;
    int STR = 5;
    int REF = 6;
    int LIST = 7;
    int MAP = 8;
    int BEAN = 9;
    int INTERFACE = 10;
    int ENUM = 11;

    static Schema create(ConfigInput input) {
        int tag = input.readInt();
        return switch (tag) {
            case BOOL -> SchemaPrimitive.SBool;
            case INT -> SchemaPrimitive.SInt;
            case LONG -> SchemaPrimitive.SLong;
            case FLOAT -> SchemaPrimitive.SFloat;
            case STR -> SchemaPrimitive.SStr;
            case REF -> new SchemaRef(input);
            case LIST -> new SchemaList(input);
            case MAP -> new SchemaMap(input);
            case BEAN -> new SchemaBean(input);
            case INTERFACE -> new SchemaInterface(input);
            case ENUM -> new SchemaEnum(input);
            default -> throw new ConfigErr("schema tag " + tag + " not supported");
        };
    }
}
