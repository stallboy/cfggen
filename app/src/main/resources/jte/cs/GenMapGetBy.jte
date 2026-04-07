@import configgen.schema.*
@import configgen.gencs.StructModel
@param StructModel model
@param KeySchema keySchema
@param boolean isPrimaryKey

    @if(keySchema.fields().size() > 1)
        !{String keyClassName = model.keyClassName(keySchema);}
        class ${keyClassName}
        {
            @for(FieldSchema field : keySchema.fieldSchemas())
                readonly ${model.type(field.type())} ${model.upper1(field.name())};
            @endfor

            public ${keyClassName}(${model.formalParams(keySchema.fieldSchemas())})
            {
                @for(FieldSchema field : keySchema.fieldSchemas())
                    ${model.upper1(field.name())} = ${model.lower1(field.name())};
                @endfor
            }

            public override int GetHashCode()
            {
                return ${model.hashCodes(keySchema.fieldSchemas())};
            }

            public override bool Equals(object? obj)
            {
                if (obj == null) return false;
                if (obj == this) return true;
                var o = obj as ${keyClassName};
                return o != null && ${model.equals(keySchema.fieldSchemas())};
            }
        }

    @endif

    !{String className =  model.name.className + (model.isEnum() ? "Info" : "");}
    !{String mapName = isPrimaryKey ? "_all" : model.uniqueKeyMapName(keySchema);}
    !{String allType = "OrderedDictionary<" + model.keyClassName(keySchema) + ", " + className + ">";}
    !{String getByName = isPrimaryKey ? "Get" : model.uniqueKeyGetByName(keySchema);}
    private static ${allType} ${mapName} = [];

    public static ${className}? ${getByName}(${model.formalParams(keySchema.fieldSchemas())})
    {
        return ${mapName}.GetValueOrDefault(${model.actualParamsKey(keySchema)});
    }
