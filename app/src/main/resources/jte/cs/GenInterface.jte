@import configgen.gencs.InterfaceModel
@import configgen.schema.HasRef
@import configgen.schema.StructSchema
@param InterfaceModel model
@if(!model.topPkg.equals("Config"))
    using Config;
@endif

namespace ${model.name.pkg};

public abstract class ${model.name.className}
{
    @if(model.sInterface.nullableEnumRefTable() != null)
        public abstract ${model.fullName(model.sInterface.nullableEnumRefTable())} type();
    @endif


    @if(HasRef.hasRef(model.sInterface))
        internal virtual void _resolve(LoadErrors errors)
        {
        }
    @endif

    internal static ${model.name.className} _create(Stream os)
    {
        var impl = os.ReadStringInPool();
        switch(impl)
        {
            @for(StructSchema impl : model.sInterface.impls())
                case "${impl.name()}":
                    return ${model.fullName(impl)}._create(os);
            @endfor
        }
        throw os.NotFoundImpl(impl, "${model.sInterface.name()}");
    }
}

