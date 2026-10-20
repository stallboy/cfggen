@import configgen.genjava.code.StructuralClassModel
@import configgen.genjava.GenJavaUtil
@import configgen.schema.*
@import configgen.util.StringUtil
@param StructuralClassModel model
    @if(model.isImpl)
    @Override
    @endif
    public void _collectExtensions(java.util.function.Consumer<${model.codeTopPkg}.ConfigExtension> collect) {
        @if(!model.javaBase.isEmpty())
        collect.accept(this);
        @endif
        @for(FieldSchema field : model.structural.fields())
            !{String n = StringUtil.lower1(field.name());}
            @if(field.type() instanceof FieldType.StructRef && GenJavaUtil.hasExtension(field.type()))
                ${n}._collectExtensions(collect);
            @elseif(field.type() instanceof FieldType.FList list && GenJavaUtil.hasExtension(list.item()))
                for (var child : ${n}) {
                    child._collectExtensions(collect);
                }
            @elseif(field.type() instanceof FieldType.FMap map)
                @if(GenJavaUtil.hasExtension(map.key()))
                    for (var child : ${n}.keySet()) {
                        child._collectExtensions(collect);
                    }
                @endif
                @if(GenJavaUtil.hasExtension(map.value()))
                    for (var child : ${n}.values()) {
                        child._collectExtensions(collect);
                    }
                @endif
            @endif
        @endfor
    }
