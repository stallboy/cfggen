@import configgen.gengo.GoName
@import configgen.gengo.model.InterfaceModel
@import configgen.schema.Structural
@param InterfaceModel model
package ${model.pkg}
!{String className = model.name.className;}


type ${className} interface{}

func create${className}(stream *Stream) ${className} {
    var typeName = stream.ReadString()
    switch typeName {
    @for(Structural impl : model.sInterface.impls())
    case "${impl.name()}":
        return create${new GoName(impl).className}(stream)
    @endfor
    default:
        panic("unexpected ${className} type: " + typeName)
    }
}
