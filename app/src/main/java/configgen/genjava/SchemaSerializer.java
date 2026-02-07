package configgen.genjava;

public final class SchemaSerializer {
    private final ConfigOutput output;

    public SchemaSerializer(ConfigOutput output) {
        this.output = output;
    }

    public void serialize(Schema schema) {
        switch (schema) {
            case SchemaPrimitive primitive -> serializePrimitive(primitive);
            case SchemaRef ref -> serializeRef(ref);
            case SchemaList list -> serializeList(list);
            case SchemaMap map -> serializeMap(map);
            case SchemaBean bean -> serializeBean(bean);
            case SchemaEnum enumSchema -> serializeEnum(enumSchema);
            case SchemaInterface interfaceSchema -> serializeInterface(interfaceSchema);
        }
    }

    private void serializePrimitive(SchemaPrimitive primitive) {
        output.writeInt(switch (primitive) {
            case SBool -> Schema.BOOL;
            case SInt -> Schema.INT;
            case SLong -> Schema.LONG;
            case SFloat -> Schema.FLOAT;
            case SStr -> Schema.STR;
        });
    }

    private void serializeRef(SchemaRef ref) {
        output.writeInt(Schema.REF);
        output.writeStr(ref.type);
    }

    private void serializeList(SchemaList list) {
        output.writeInt(Schema.LIST);
        serialize(list.ele());
    }

    private void serializeMap(SchemaMap map) {
        output.writeInt(Schema.MAP);
        serialize(map.key());
        serialize(map.value());
    }

    private void serializeBean(SchemaBean bean) {
        output.writeInt(Schema.BEAN);
        output.writeBool(bean.isTable);
        output.writeInt(bean.columns.size());
        for (SchemaBean.Column column : bean.columns) {
            output.writeStr(column.name());
            serialize(column.schema());
        }
    }

    private void serializeEnum(SchemaEnum enumSchema) {
        output.writeInt(Schema.ENUM);
        output.writeBool(enumSchema.isEnumPart);
        output.writeBool(enumSchema.hasIntValue);
        output.writeInt(enumSchema.values.size());
        for (var entry : enumSchema.values.entrySet()) {
            output.writeStr(entry.getKey());
            if (enumSchema.hasIntValue) {
                output.writeInt(entry.getValue());
            }
        }
    }

    private void serializeInterface(SchemaInterface interfaceSchema) {
        output.writeInt(Schema.INTERFACE);
        output.writeInt(interfaceSchema.implementations.size());
        for (var entry : interfaceSchema.implementations.entrySet()) {
            output.writeStr(entry.getKey());
            serialize(entry.getValue());
        }
    }
}
