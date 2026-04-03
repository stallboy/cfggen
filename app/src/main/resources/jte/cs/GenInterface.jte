@import configgen.gencs.InterfaceModel
@import configgen.schema.StructSchema
@param InterfaceModel model
@if(!model.topPkg.equals("Config"))
using Config;
@endif

namespace ${model.name.pkg};

public partial interface ${model.name.className}
{
    @if(model.sInterface.nullableEnumRefTable() != null)
        public ${model.fullName(model.sInterface.nullableEnumRefTable())} type();
    @endif
}
