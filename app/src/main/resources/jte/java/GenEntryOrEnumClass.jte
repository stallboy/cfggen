@import configgen.genjava.code.EntryOrEnumModel
@import configgen.genjava.GenJavaUtil
@import configgen.genjava.code.Name
@import configgen.schema.FieldSchema
@import configgen.schema.ForeignKeySchema
@import configgen.util.StringUtil
@param EntryOrEnumModel model
package ${model.pkg};

!{
    String errorsClass = GenJavaUtil.loadValueErrsClass(model.className);
    String lookup = Name.GetByKeyFunctionNameInConfigMgr(model.name.cfg, model.table.primaryKey(), true, model.table);
}
@if(model.isNeedReadData && errorsClass.equals("LoadValueErrs"))
import configgen.genjava.LoadValueErrs;

@endif
@if(!model.sourceComment.isEmpty())
${model.sourceComment}
@endif
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
                ${model.enumFieldName(enumName)}("${enumName}"${model.hasFixedId ? ", " + model.table.meta().assignedEnumId(enumName) : ""})${fix}
            @else
                public static final ${model.className} ${model.enumFieldName(enumName)} = new ${model.className}(${c - 1}, "${enumName}"${model.hasFixedId ? ", " + model.table.meta().assignedEnumId(enumName) : ""});
            @endif
        @endfor
        @if(model.isEnum && 0 == c)
            ;
        @endif


        private final String value;


        ${model.className}(${model.isEnum ? "" : "int index, "}String value${model.hasFixedId ? ", int fixedId" : ""}) {
            @if(!model.isEnum)
                this.index = index;
            @endif
            this.value = value;
            @if(model.hasFixedId)
                this.fixedId = fixedId;
            @endif
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
                ${model.enumFieldName(enumName)}("${enumName}", ${value}${model.hasFixedId ? ", " + model.table.meta().assignedEnumId(enumName) : ""})${fix}
            @else
                public static final ${model.className} ${model.enumFieldName(enumName)} = new ${model.className}(${c - 1}, "${enumName}", ${value}${model.hasFixedId ? ", " + model.table.meta().assignedEnumId(enumName) : ""});
            @endif
        @endfor
        @if(model.isEnum && 0 == c)
            ;
        @endif


        private final String name;
        private final int value;


        ${model.className}(${model.isEnum ? "" : "int index, "}String name, int value${model.hasFixedId ? ", int fixedId" : ""}) {
            @if(!model.isEnum)
                this.index = index;
            @endif
            this.name = name;
            this.value = value;
            @if(model.hasFixedId)
                this.fixedId = fixedId;
            @endif
        }

    @endif

    @if(!model.isEnum)
        private final int index;

    @endif
    @if(model.hasFixedId)
        private final int fixedId;

    @endif
    @if(model.isEnum)
        public static final java.util.Map<${model.hasNoIntValue ? "String" : "Integer"}, ${model.className}> map;

        static {
            var symbols = new java.util.LinkedHashMap<${model.hasNoIntValue ? "String" : "Integer"}, ${model.className}>();
            for(${model.className} e : ${model.className}.values()) {
                symbols.put(e.value, e);
            }
            map = java.util.Collections.unmodifiableMap(symbols);
        }

        public static ${model.className} get(${model.hasNoIntValue ? "String" : "int"} value) {
            return map.get(value);
        }

        @for(FieldSchema field : model.table.fields())
            @if(!field.comment().isEmpty())
                /**
                @for(String line : field.comment().split("\n")) * ${line}@endfor
                 */
            @endif
            public ${model.type(field.type())} get${StringUtil.upper1(field.name())}() {
                @if(field == model.table.primaryKey().fieldSchemas().getFirst())
                    return value;
                @elseif(field == model.entryBase.fieldSchema())
                    return name;
                @else
                    return ref().get${StringUtil.upper1(field.name())}();
                @endif
            }

        @endfor

        @for(ForeignKeySchema fk : model.table.foreignKeys())
            public ${model.refType(fk)} ${StringUtil.lower1(Name.refName(fk))}() {
                return ref().${StringUtil.lower1(Name.refName(fk))}();
            }

        @endfor
    @endif

    @if(model.isNeedReadData)
        public ${model.dataNameFullName} ref() {
            return ref(${model.codeTopPkg}.ConfigMgr.getMgr());
        }

        public ${model.dataNameFullName} ref(${model.codeTopPkg}.ConfigMgr mgr) {
            return mgr._get_${model.name.containerPrefix}Refs(${model.isEnum ? "ordinal()" : "index"});
        }

        public static void setAllRefs(${model.codeTopPkg}.ConfigMgr mgr, ${errorsClass} errors) {
            mgr._set_${model.name.containerPrefix}Refs(new ${model.dataNameFullName}[] {
                @for(String enumName : model.enumNames)
                    mgr.${lookup}(${model.enumFieldName(enumName)}.value),
                @endfor
            });
            for (var row : mgr._get_${model.name.containerPrefix}All()${model.table.primaryKey().fieldSchemas().getFirst().isSeq() ? "" : ".values()"}) {
                var key = row.get${StringUtil.upper1(model.table.primaryKey().fieldSchemas().getFirst().name())}();
                String symbol = row.get${StringUtil.upper1(model.entryBase.fieldSchema().name())}();
                @if(!model.isEnum)
                    if (symbol.isEmpty()) {
                        continue;
                    }
                @endif
                @if(model.isEnum && model.hasNoIntValue)
                    ${model.className} expected = get(symbol);
                @else
                    ${model.className} expected = switch (symbol) {
                        @for(String enumName : model.enumNames)
                            case "${enumName}" -> ${model.enumFieldName(enumName)};
                        @endfor
                        default -> null;
                    };
                @endif
                if (expected == null) {
                    errors.unknownSymbol("${model.table.name()}", symbol, key, mgr.explain("${model.table.name()}", key));
                    continue;
                }
                @if(!model.hasNoIntValue)
                    if (key != expected.value) {
                        errors.changedSymbolId("${model.table.name()}", symbol, expected.value, key, mgr.explain("${model.table.name()}", key));
                    }
                @endif
                @if(model.hasFixedId)
                    if (row.getId() != expected.fixedId) {
                        errors.changedSymbolId("${model.table.name()}", symbol, expected.fixedId, row.getId(), mgr.explain("${model.table.name()}", key));
                    }
                @endif
            }
            @if(model.isEnum)
                for (${model.className} e : map.values()) {
                    if (e.ref(mgr) == null) {
                        errors.missingSymbol("${model.table.name()}", ${model.hasNoIntValue ? "e.value" : "e.name"}, e.value);
                    }
                }
            @else
                @for(String enumName : model.enumNames)
                    if (${model.enumFieldName(enumName)}.ref(mgr) == null) {
                        errors.missingSymbol("${model.table.name()}", "${enumName}", ${model.enumFieldName(enumName)}.value);
                    }
                @endfor
            @endif
        }

    @endif
}
