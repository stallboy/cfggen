@import configgen.gengo.GoName
@import configgen.gengo.InterfaceModel
@import configgen.schema.Structural
@param InterfaceModel model
package ${model.pkg()}
!{String className = model.name().className;}


type ${className} interface{}

func create${className}(stream *Stream) ${className} {
    var typeName = stream.ReadStringInPool()
    @if(model.sInterface().impls().size() == 1)
        !{var impl = model.sInterface().impls().getFirst();}
        if typeName == "${impl.name()}" { return create${new GoName(impl).className}(stream) }
    @elseif(model.sInterface().impls().size() > 1)
        switch typeName {
        @for(Structural impl : model.sInterface().impls())
        case "${impl.name()}":
            return create${new GoName(impl).className}(stream)
        @endfor
        }
    @endif
    panic("unexpected ${className} type: " + typeName)
}
