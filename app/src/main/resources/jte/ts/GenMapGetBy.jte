
@import configgen.schema.*
@import configgen.gents.StructModel
@param StructModel model
@param KeySchema keySchema
@param boolean isPrimaryKey

    !{String mapName = isPrimaryKey ? "all" : model.uniqueKeyMapName(keySchema);}
    !{String className = model.structClassName;}

    !{String getByName = isPrimaryKey ? "Get" : model.uniqueKeyGetByName(keySchema);}
    private static ${mapName}: Map<${model.mapKeyType(keySchema)}, ${className}>;

    static ${getByName}(${model.formalParams(keySchema.fieldSchemas())}) : ${className} | undefined {
        return this.${mapName}.get(${model.actualParamsKey(keySchema)})
    }
