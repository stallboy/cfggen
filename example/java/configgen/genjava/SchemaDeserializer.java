package configgen.genjava;

public final class SchemaDeserializer {

    public static Schema deserialize(ConfigInput input) {
        var errors = input.errors();
        Schema schema = new SchemaDeserializer(input).deserialize();
        errors.checkErrors();
        return schema;
    }

    private final ConfigInput input;

    private SchemaDeserializer(ConfigInput input) {
        this.input = input;
    }

    private Schema deserialize() {
        int tag = input.readInt();
        return switch (tag) {
            case Schema.BOOL -> SchemaPrimitive.SBool;
            case Schema.INT -> SchemaPrimitive.SInt;
            case Schema.LONG -> SchemaPrimitive.SLong;
            case Schema.FLOAT -> SchemaPrimitive.SFloat;
            case Schema.STR -> SchemaPrimitive.SStr;
            case Schema.TEXT -> SchemaPrimitive.SText;
            case Schema.REF -> deserializeRef();
            case Schema.LIST -> deserializeList();
            case Schema.MAP -> deserializeMap();
            case Schema.BEAN -> deserializeBean();
            case Schema.INTERFACE -> deserializeInterface();
            case Schema.ENUM -> deserializeEnum();
            default -> throw input.errors().unsupportedSchemaTag(tag);
        };
    }

    private SchemaRef deserializeRef() {
        String type = input.readString();
        return new SchemaRef(type);
    }

    private SchemaList deserializeList() {
        Schema element = deserialize();
        return new SchemaList(element);
    }

    private SchemaMap deserializeMap() {
        Schema key = deserialize();
        Schema value = deserialize();
        return new SchemaMap(key, value);
    }

    private SchemaBean deserializeBean() {
        boolean isTable = input.readBool();
        SchemaBean bean = new SchemaBean(isTable);
        int size = input.readCount(8);
        var names = new java.util.HashSet<String>();
        for (int i = 0; i < size; i++) {
            String name = input.readString();
            if (!names.add(name)) input.errors().duplicateKey("schema field", name);
            Schema schema = deserialize();
            bean.addColumn(name, schema);
        }
        return bean;
    }

    private SchemaEnum deserializeEnum() {
        boolean isEnumPart = input.readBool();
        boolean hasIntValue = input.readBool();
        SchemaEnum enumSchema = new SchemaEnum(isEnumPart, hasIntValue);
        int size = input.readCount(hasIntValue ? 8 : 4);
        for (int i = 0; i < size; i++) {
            String name = input.readString();
            if (enumSchema.values.containsKey(name)) input.errors().duplicateKey("schema symbol", name);
            if (hasIntValue) {
                int value = input.readInt();
                enumSchema.addValue(name, value);
            } else {
                enumSchema.addValue(name);
            }
        }
        return enumSchema;
    }

    private SchemaInterface deserializeInterface() {
        SchemaInterface interfaceSchema = new SchemaInterface();
        int size = input.readCount(8);
        for (int i = 0; i < size; i++) {
            String name = input.readString();
            Schema schema = deserialize();
            if (interfaceSchema.implementations.putIfAbsent(name, schema) != null) input.errors().duplicateKey("schema implementation", name);
        }
        return interfaceSchema;
    }
}
