@import configgen.gencs.ProcessorModel
@import configgen.naming.GenNaming
@import configgen.schema.RefKey
@import configgen.schema.TableSchema
@import configgen.util.StringUtil
@param ProcessorModel model
private static void ResolveRelations(ConfigMgr config, LoadErrors errors)
{
    var resolver = new RelationResolver(errors, new RelationResolver.Shape[] {
        @for(var type : model.relations)
        new RelationResolver.Shape("${type.owner().fullName()}", typeof(${model.fullName(type.owner())}),
            value => { var row = (${model.fullName(type.owner())})value; return new object[] { ${type.owner().fields().stream().map(model::relationField).collect(java.util.stream.Collectors.joining(", "))} }; },
            new RelationResolver.Index[] {
                @for(var index : type.indexes())
                new RelationResolver.Index(${index.field()}, ${model.ints(index.keys())}),
                @endfor
            },
            new RelationResolver.Plan[] {
                @for(var plan : type.plans())
                new RelationResolver.Plan("${plan.root()}", ${plan.slot()}, "${plan.reference().name()}", ${((RefKey.RefSimple) plan.reference().refKey()).nullable()},
                    new RelationResolver.Source[] {
                        @for(var source : plan.sources())
                        new RelationResolver.Source("${source.scope()}", ${model.ints(source.fields())}),
                        @endfor
                    }, "${plan.target()}", ${model.ints(plan.recordKey())}, new RelationResolver.Step[] {
                        @for(var step : plan.steps())
                        new RelationResolver.Step(${step.field()}, ${step.kind()}, ${model.ints(step.keys())}),
                        @endfor
                    }),
                @endfor
            }, (value, refs) => {
                @if(!type.references().isEmpty())
                var row = (${model.fullName(type.owner())})value;
                @endif
                @for(int i = 0; i < type.references().size(); i++)
                !{var fk = type.references().get(i);}
                row.${GenNaming.refFieldName(fk)} = (${model.fullName(fk.referenceResultType())})refs[${i}]!;
                @endfor
            }),
        @endfor
    });
    @for(TableSchema table : model.tableSchemas)
    resolver.Table("${table.name()}", ${model.fullName(table)}.All(config));
    @endfor
    resolver.Resolve();
}
