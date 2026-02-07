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
    int REF = 6;
    int LIST = 7;
    int MAP = 8;
    int BEAN = 9;
    int INTERFACE = 10;
    int ENUM = 11;
}
