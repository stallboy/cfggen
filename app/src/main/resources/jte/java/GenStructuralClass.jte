@import configgen.genjava.code.Name
@import configgen.genjava.code.TypeStr
@import configgen.gen.Generator
@import configgen.genjava.code.ResolveModel
@import configgen.genjava.code.StructuralClassModel
@import configgen.genjava.code.StructuralClassModel.FieldInfo
@import configgen.genjava.code.StructuralClassModel.ForeignKeyInfo
@import configgen.schema.*
@param StructuralClassModel model
package ${model.pkg};

@if(model.isImpl)
    public ${model.isSealedInterface?"final ":""}final class ${model.className} implements ${model.nullableInterfaceFullName} {
    @if(model.enumRefTable != null)
        @Override
        public ${Name.refType(model.enumRefTable)} type() {
            return ${Name.refType(model.enumRefTable)}.${model.structural.name().toUpperCase()};
        }
    @endif
@else
    public class ${model.className} {
@endif

    @for(FieldInfo field : model.fields)
        private ${field.type()} ${field.name()};
    @endfor


    @if(!model.isTableAndNeedBuilder)
        @for(ForeignKeyInfo fk : model.foreignKeys)
        private ${fk.type()} ${fk.name()};
        @endfor

    @endif

    @if(!model.isStructAndHasNoField)
        private ${model.className}() {
        }

    @endif

    @if(!model.isTable)
        public ${model.className}(${model.formalParams()}) {
            @for(FieldInfo field : model.fields)
            this.${field.name()} = ${field.name()};
            @endfor
        }

    @elseif(model.isTableAndNeedBuilder)
        public ${model.className}(${model.className}Builder b) {
            @for(FieldInfo field : model.fields)
                this.${field.name()} = b.${field.name()};
            @endfor
        }

    @endif

    public static ${model.className} _create(configgen.genjava.ConfigInput input) {
        ${model.className} self = new ${model.className}();
        @for(FieldSchema field : model.structural.fields())
            !{
                String ln = Generator.lower1(field.name());
            }
            @if(field.type() instanceof FieldType.SimpleType simpleType)
                self.${ln} = ${TypeStr.readValue(simpleType)};
            @elseif(field.type() instanceof FieldType.FList fList)
                {
                    int c = input.readInt();
                    if (c == 0) {
                        self.${ln} = java.util.Collections.emptyList();
                    } else {
                        self.${ln} = new java.util.ArrayList<>(c);
                        for (; c > 0; c--) {
                            self.${ln}.add(${TypeStr.readValue((fList.item()))});
                        }
                    }
                }
            @elseif(field.type() instanceof FieldType.FMap fMap)
                {
                    int c = input.readInt();
                    if (c == 0) {
                        self.${ln} = java.util.Collections.emptyMap();
                    } else {
                        self.${ln} = new java.util.LinkedHashMap<>(c);
                        for (; c > 0; c--) {
                            self.${ln}.put(${TypeStr.readValue(fMap.key())}, ${TypeStr.readValue((fMap.value()))});
                        }
                    }
                }
            @endif
        @endfor
        return self;
    }

    @for(FieldInfo field : model.fields)
        @if(!field.comment().isEmpty())
        /**
         * ${field.comment()}
         */
        @endif
        public ${field.type()} get${Generator.upper1(field.name())}() {
            return ${field.name()};
        }

    @endfor

    @if(!model.isTableAndNeedBuilder)
        @for(ForeignKeyInfo fk : model.foreignKeys)
            public ${fk.type()} ${Generator.lower1(fk.name())}() {
                return ${fk.name()};
            }

        @endfor
    @endif

    @if(model.isStructAndHasNoField)
        @Override
        public int hashCode() {
            return ${model.className}.class.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ${model.className};
        }
        @elseif(!model.isTable)
        @Override
        public int hashCode() {
            return ${model.hashCodes()};
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ${model.className}))
                return false;
            ${model.className} o = (${model.className}) other;
            return ${model.equals()};
        }

    @endif

    @Override
    public String toString() {
        @if(model.isStructAndHasNoField)
        return "${model.isImpl ? model.className : ""}";
        @else
        return "${model.isImpl ? model.className : ""}(" + ${model.toStringParams()} + ")";
        @endif
    }

    @if(model.hasRef && !model.isTableAndNeedBuilder)
        @template.java.GenResolve(model = new ResolveModel(model.structural, model.nullableInterface))
    @endif

    @if(model.isTable)
        @template.java.GenStructuralClassTablePart(model = model)
    @endif
}
