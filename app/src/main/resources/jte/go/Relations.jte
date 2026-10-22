@import configgen.gengo.CfgMgrModel
@import configgen.gengo.GoName
@import configgen.gengo.StructModel
@import configgen.naming.GenNaming
@import configgen.schema.RefKey
@import configgen.schema.InterfaceSchema
@import configgen.util.StringUtil
@param CfgMgrModel model
// All navigation state is temporary and local to this load. Only typed targets remain in records.
func (stream *Stream) bindObjectReferences() {
    type source struct { scope string; fields []int }
    type step struct { field, kind int; keys []int }
    type plan struct { root string; slot int; name string; nullable bool; sources []source; target string; recordKey []int; steps []step }
    type index struct { field int; keys []int }
    type shape struct { name string; fields func(any) []any; indexes []index; plans []plan; bind func(any, []any) }
    type listView struct { rows []any }
    type mapView struct { entries [][2]any; lookup map[string]any }
    type occurrence struct { value, root any; table string; parent *occurrence }
    shapes := map[string]*shape {
        @for(var type : model.relations())
        "${type.owner().fullName()}": {
            name: "${type.owner().fullName()}",
            fields: func(value any) []any {
                @if(!type.owner().fields().isEmpty())
                row := value.(*${new GoName(type.owner()).className})
                @endif
                return []any { ${type.owner().fields().stream().map(model::relationField).collect(java.util.stream.Collectors.joining(", "))} }
            },
            indexes: []index {
                @for(var index : type.indexes())
                { ${index.field()}, ${model.ints(index.keys())} },
                @endfor
            },
            plans: []plan {
                @for(var plan : type.plans())
                { "${plan.root()}", ${plan.slot()}, "${plan.reference().name()}", ${((RefKey.RefSimple)plan.reference().refKey()).nullable()}, []source {
                    @for(var source : plan.sources())
                    { "${source.scope()}", ${model.ints(source.fields())} },
                    @endfor
                }, "${plan.target()}", ${model.ints(plan.recordKey())}, []step {
                    @for(var step : plan.steps())
                    { ${step.field()}, ${step.kind()}, ${model.ints(step.keys())} },
                    @endfor
                } },
                @endfor
            },
            bind: func(value any, refs []any) {
                @if(!type.references().isEmpty())
                row := value.(*${new GoName(type.owner()).className})
                @endif
                @for(int i = 0; i < type.references().size(); i++)
                !{var fk = type.references().get(i);}
                if refs[${i}] == nil { row.${StringUtil.lower1(GenNaming.refFieldName(fk))} = nil } else {
                    row.${StringUtil.lower1(GenNaming.refFieldName(fk))} = refs[${i}].(${fk.referenceResultType() instanceof InterfaceSchema ? "" : "*"}${new GoName(fk.referenceResultType()).className})
                }
                @endfor
            },
        },
        @endfor
    }
@template.go.RelationResolver(model = model)

