@import configgen.schema.*
@import configgen.gencs.StructModel
@param StructModel model
@param KeySchema keySchema
@param boolean isPrimaryKey

    @if(keySchema.fields().size() > 1)
        !{String keyClassName = model.keyClassName(keySchema);}
        public sealed record ${keyClassName}(${keySchema.fieldSchemas().stream().map(field -> model.type(field.type()) + " " + model.upper1(field.name())).collect(java.util.stream.Collectors.joining(", "))});

    @endif
    !{String className =  model.dictValueType();}
    !{String getByName = isPrimaryKey ? "Get" : model.uniqueKeyGetByName(keySchema);}
    @if(model.isSeqKey(keySchema))
    public static ${className}? ${getByName}(${model.formalParams(keySchema.fieldSchemas())})
        => ${getByName}(ConfigMgr.GetMgr(), ${keySchema.fields().stream().map(StructModel::_lower1).collect(java.util.stream.Collectors.joining(", "))});

    public static ${className}? ${getByName}(ConfigMgr config, ${model.formalParams(keySchema.fieldSchemas())})
    {
        var index = config.${model.indexField(keySchema)};
        var key = ${model.actualParamsKey(keySchema)};
        return key >= 0 && key < index.Length ? index[key] : null;
    }
    @else
    public static ${className}? ${getByName}(${model.formalParams(keySchema.fieldSchemas())})
        => ${getByName}(ConfigMgr.GetMgr(), ${keySchema.fields().stream().map(StructModel::_lower1).collect(java.util.stream.Collectors.joining(", "))});

    public static ${className}? ${getByName}(ConfigMgr config, ${model.formalParams(keySchema.fieldSchemas())})
    {
        var index = config.${model.indexField(keySchema)};
        return index.TryGetValue(${model.actualParamsKey(keySchema)}, out var result) ? result : null;
    }
    @endif
