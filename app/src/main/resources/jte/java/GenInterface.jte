@import configgen.genjava.code.InterfaceModel
@param InterfaceModel model
package ${model.pkg};

@if(model.isSealedInterface)
public sealed interface ${model.className} permits ${String.join(", ", model.impls.stream().map(InterfaceModel.Impl::upper1Name).toList())} {
@else
public interface ${model.className} {
@endif
    @if(model.nullableEnumRefTable != null)
    ${model.nullableEnumRefTable} type();

    @endif

    @if(model.hasRef)
    default void _resolve(${model.codeTopPkg}.ConfigMgr mgr) {
    }

    @endif

    static ${model.className} _create(configgen.genjava.ConfigInput input) {
        switch (input.readStr()) {
            @for(InterfaceModel.Impl impl : model.impls)
            case "${impl.name()}":
                return ${impl.fullName()}._create(input);
            @endfor
        }
        throw new IllegalArgumentException();
    }
}
