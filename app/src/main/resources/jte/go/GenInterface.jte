@import configgen.gengo.GoName
@import configgen.gengo.model.InterfaceModel
@import configgen.schema.Structural
@param InterfaceModel model
package ${model.pkg}

type ${model.className} interface{}

func create${model.className}(stream *Stream) ${model.className} {
    var typeName = stream.ReadString()
    switch typeName {
    @for(Structural impl : model.sInterface.impls())
    case "${impl.name()}":
        return create${new GoName(impl).className}(stream)
    @endfor
    default:
        panic("unexpected ${model.className} type: " + typeName)
    }
}
