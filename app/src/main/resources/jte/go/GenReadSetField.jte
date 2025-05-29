@import configgen.schema.*
@import configgen.gengo.GenGo
@param FieldType fieldType
@param String varName
@if (fieldType instanceof FieldType.StructRef structRef)
    v.${varName} = create${GenGo.ClassName(structRef.obj())}(stream)
@elseif(fieldType instanceof FieldType.FList fList)
    ${varName}Size := stream.ReadInt32()
    v.${varName} = make([]${GenGo.type(fList.item())}, ${varName}Size)
    for i := 0; i < int(${varName}Size); i++ {
    v.${varName}[i] = @template.go.genReadField(fieldType=fList.item())
    }
@elseif (fieldType instanceof FieldType.FMap fMap)
    v.${varName} = make(map[${GenGo.type(fMap.key())}]${GenGo.type(fMap.value())}, ${varName}Size)
    for i := 0; i < int(${varName}Size); i++ {
    var k = @template.go.genReadField(fieldType=fMap.key())
    v.${varName}[k] = @template.go.genReadField(fieldType=fMap.value())
    }
@else
    v.${varName} = @template.go.genReadField(fieldType=fieldType)
@endif