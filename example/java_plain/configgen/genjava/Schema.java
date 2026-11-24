package configgen.genjava;

public sealed interface Schema permits SchemaBean, SchemaEnum, SchemaInterface, SchemaList, SchemaMap, SchemaPrimitive, SchemaRef {
    /**
     * 比较兼容性，codeSchema.compatible(dataSchema);
     * 参照example/javaload/LoadConfig.java
     */
    boolean compatible(Schema other);

    int BOOL = 1;
    int INT = 2;
    int LONG = 3;
    int FLOAT = 4;
    int STR = 5;
    int TEXT = 6;
    int REF = 7;
    int LIST = 8;
    int MAP = 9;
    int BEAN = 10;
    int INTERFACE = 11;
    int ENUM = 12;
}
