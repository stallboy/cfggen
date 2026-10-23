@import configgen.gencs.ProcessorModel
@import configgen.naming.GenNaming
@import configgen.schema.TableSchema
@param ProcessorModel model
    private static void ResolveRelations(ConfigMgr config, LoadErrors errors)
    {
        var resolver = new RelationResolver(errors);
        @for(var type : model.relations)
        resolver.Type<${model.fullName(type.owner())}>("${type.owner().fullName()}",
                row => new object[] { ${type.owner().fields().stream().map(model::relationField).collect(java.util.stream.Collectors.joining(", "))} })${type.indexes().isEmpty() && type.plans().isEmpty() ? ";" : ""}
            @for(var index : type.indexes())
                .Index(${index.field()}${model.arguments(index.keys())})${index == type.indexes().getLast() && type.plans().isEmpty() ? ";" : ""}
            @endfor
            @for(var plan : type.plans())
                .Bind(${model.relationPlan(plan)},
                    (row, reference) => row.${GenNaming.refFieldName(plan.reference())} = (${model.fullName(plan.reference().referenceResultType())})reference!)${plan == type.plans().getLast() ? ";" : ""}
            @endfor
        @endfor
        @for(TableSchema table : model.relationTables)
        resolver.Table("${table.name()}", ${model.fullName(table)}.All(config));
        @endfor
        resolver.Resolve();
    }
