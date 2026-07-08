@import configgen.gencs.InterfaceModel
@import configgen.schema.StructSchema
@param InterfaceModel model
@if(model.unity)
using System;
using System.Collections.Generic;
@endif

${model.nsLine()}

public partial interface ${model.name.className}
{
    @if(model.sInterface.nullableEnumRefTable() != null)
        public ${model.fullName(model.sInterface.nullableEnumRefTable())} type();
    @endif
}
@if(model.unity)
}
@endif
