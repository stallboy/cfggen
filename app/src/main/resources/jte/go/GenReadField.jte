@import configgen.schema.FieldType
@import configgen.gengo.GenGo
@param FieldType fieldType
@if (fieldType instanceof FieldType.StructRef structRef)create${GenGo.ClassName(structRef.obj())}(stream)
@elseif (fieldType instanceof FieldType.FList)
@elseif (fieldType instanceof FieldType.FMap)
@else
stream.Read${GenGo.upper1(GenGo.type(fieldType))}()@endif