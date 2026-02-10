@import configgen.genjava.code.NameableName
@import configgen.genjava.code.TypeStr
@import configgen.schema.*
@import configgen.gen.Generator
@import configgen.util.StringUtil
@param TableSchema table
@param NameableName name
package ${name.pkg};

public class ${name.className}Builder {
    @for(FieldSchema field : table.fields())
        public ${TypeStr.type(field.type())} ${StringUtil.lower1(field.name())};
    @endfor


    public ${name.className} build() {
        @for(FieldSchema field : table.fields())
            @if(!TypeStr.isJavaPrimitive(field.type()))
                !{
                    String fn = StringUtil.lower1(field.name());
                }
                @if(field.type() instanceof FieldType.StructRef)
                    java.util.Objects.requireNonNull(${fn});
                @else
                    if (${fn} == null) {
                        ${fn} = ${TypeStr.defaultValue(field.type())};
                    }
                @endif
            @endif
        @endfor
        return new ${name.className}(this);
    }

}
