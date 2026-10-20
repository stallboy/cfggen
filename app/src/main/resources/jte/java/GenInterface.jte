@import configgen.genjava.code.InterfaceModel
@import configgen.genjava.GenJavaUtil
@param InterfaceModel model
package ${model.pkg};

!{ String errorsClass = GenJavaUtil.loadValueErrsClass(model.className); }
@if(model.hasRef && errorsClass.equals("LoadValueErrs"))
import configgen.genjava.LoadValueErrs;

@endif

@if(model.isSealedInterface && !model.impls.isEmpty())
public sealed interface ${model.className} permits ${String.join(", ", model.impls.stream().map(InterfaceModel.Impl::className).toList())} {
@else
public interface ${model.className} {
@endif
    @if(model.nullableEnumRefTable != null)
    ${model.nullableEnumRefTable} type();

    @endif

    @if(model.hasExtension)
    default void _collectExtensions(java.util.function.Consumer<${model.codeTopPkg}.ConfigExtension> collect) { }

    @endif

    @if(model.hasRef)
    default void _resolve(${model.codeTopPkg}.ConfigMgr mgr, ${errorsClass} errors) {
    }

    @endif

    static ${model.className} _create(configgen.genjava.ConfigInput input) {
        String tag = input.readStringInPool();
        @if(model.impls.size() == 1)
            !{var impl = model.impls.getFirst();}
            if (tag.equals("${impl.name()}")) return ${impl.fullName()}._create(input);
            throw input.errors().unknownVariant("${model.className}", tag);
        @elseif(model.impls.isEmpty())
            throw input.errors().unknownVariant("${model.className}", tag);
        @else
            return switch (tag) {
                @for(InterfaceModel.Impl impl : model.impls)
                case "${impl.name()}" -> ${impl.fullName()}._create(input);
                @endfor
                default -> throw input.errors().unknownVariant("${model.className}", tag);
            };
        @endif
    }
}
