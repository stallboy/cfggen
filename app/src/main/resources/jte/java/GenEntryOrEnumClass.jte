@import configgen.genjava.code.EntryOrEnumModel
@import configgen.genjava.code.TypeStr
@import configgen.gen.Generator
@import configgen.genjava.code.Name
@import configgen.schema.FieldSchema
@import configgen.schema.ForeignKeySchema
@import configgen.util.StringUtil
@param EntryOrEnumModel model
package ${model.pkg};

public ${model.isEnum ? "enum" : "class"} ${model.className} {
    @if(model.hasNoIntValue)
        !{
            int len = 0;
            for (String enumName : model.enumNames) {
                len++;
            }
            int c = 0;
        }
        @for(String enumName : model.enumNames)
            !{
                c++;
                String fix = c == len ? ";" : ",";
            }
            @if(model.isEnum)
                ${enumName.toUpperCase()}("${enumName}")${fix}
            @else
                public static final ${model.className} ${enumName.toUpperCase()} = new ${model.className}("${enumName}");
            @endif
        @endfor
        @if(model.isEnum && 0 == c)
            ;
        @endif


        private final String value;
        @if(model.isNeedReadData)
            private volatile ${model.dataNameFullName} ref;
        @endif


        ${model.className}(String value) {
            this.value = value;
        }

    @else
        !{
            int len = model.enumNameToIntegerValueMap.size();
            int c = 0;
        }
        @for(var entry : model.enumNameToIntegerValueMap.entrySet())
            !{
                String enumName = entry.getKey();
                int value = entry.getValue();
                c++;
                String fix = c == len ? ";" : ",";
            }
            @if(model.isEnum)
                ${enumName.toUpperCase()}("${enumName}", ${value})${fix}
            @else
                public static final ${model.className} ${enumName.toUpperCase()} = new ${model.className}("${enumName}", ${value});
            @endif
        @endfor
        @if(model.isEnum && 0 == c)
            ;
        @endif


        private final String name;
        private final int value;
        @if(model.isNeedReadData)
            private volatile ${model.dataNameFullName} ref;
        @endif


        ${model.className}(String name, int value) {
            this.name = name;
            this.value = value;
        }

    @endif

    @if(model.isEnum)
        private static final java.util.Map<${model.hasNoIntValue ? "String" : "Integer"}, ${model.className}> map = new java.util.HashMap<>();

        static {
            for(${model.className} e : ${model.className}.values()) {
                map.put(e.value, e);
            }
        }

        public static ${model.className} get(${model.hasNoIntValue ? "String" : "int"} value) {
            return map.get(value);
        }

        @for(FieldSchema field : model.table.fields())
            @if(!field.comment().isEmpty())
                /**
                 * ${field.comment()}
                 */
            @endif
            public ${TypeStr.type(field.type())} get${StringUtil.upper1(field.name())}() {
                @if(field == model.table.primaryKey().fieldSchemas().get(0))
                    return value;
                @elseif(field == model.entryBase.fieldSchema())
                    return name;
                @else
                    return ref.get${StringUtil.upper1(field.name())}();
                @endif
            }

        @endfor

        @for(ForeignKeySchema fk : model.table.foreignKeys())
            public ${Name.refType(fk)} ${StringUtil.lower1(Name.refName(fk))}() {
                return ref.${StringUtil.lower1(Name.refName(fk))}();
            }

        @endfor
    @endif

    @if(model.isNeedReadData)
        public ${model.dataNameFullName} ref() {
            return ref;
        }

        void setRef(${model.codeTopPkg}.ConfigMgr mgr) {
            ref = mgr.${model.name.containerPrefix}All.get(value);
            java.util.Objects.requireNonNull(ref);
        }

        @if(model.isEnum)
            public static void setAllRefs(${model.codeTopPkg}.ConfigMgr mgr) {
                for(${model.className} e : ${model.className}.values()) {
                    e.setRef(mgr);
                }
            }
        @else
            public static void setAllRefs(${model.codeTopPkg}.ConfigMgr mgr) {
                @for(String enumName : model.enumNames)
                    ${enumName.toUpperCase()}.setRef(mgr);
                @endfor
            }
        @endif

    @endif
}
