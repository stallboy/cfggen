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
                        this.${model.upper1(field.name())} = ${model.lower1(field.name())};
                    @endfor
                }

                public override int GetHashCode()
                {
                    return ${model.hashCodes(keySchema.fieldSchemas())};
                }

                public override bool Equals(object obj)
                {
                    if (obj == null) return false;
                    if (obj == this) return true;
                    var o = obj as ${keyClassName};
                    return o != null && ${model.equals(keySchema.fieldSchemas())};
                }
            }

        @endif

        !{String mapName = isPrimaryKey ? "all" : model.uniqueKeyMapName(keySchema);}
        !{String allType = "Config.KeyedList<" + model.keyClassName(keySchema) + ", " + model.name.className + ">";}
        !{String getByName = isPrimaryKey ? "Get" : model.uniqueKeyGetByName(keySchema);}
        static ${allType} ${mapName} = null;

        public static ${model.name.className} ${getByName}(${model.formalParams(keySchema.fieldSchemas())})
        {
            ${model.name.className} v;
            return ${mapName}.TryGetValue(${model.actualParamsKey(keySchema)}, out v) ? v : null;
        }
