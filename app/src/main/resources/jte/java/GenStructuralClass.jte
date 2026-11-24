@import configgen.genjava.code.StructuralClassModel
@import configgen.genjava.code.StructuralClassModel.FieldInfo
@import configgen.genjava.code.StructuralClassModel.ForeignKeyInfo
@import configgen.genjava.code.TypeStr
@import configgen.schema.*
@import configgen.util.StringUtil
@import configgen.genjava.GenJavaUtil
@param StructuralClassModel model
package ${model.pkg};

@if(model.cfg.jspecify())
import org.jspecify.annotations.Nullable;

@endif
!{ String errorsClass = GenJavaUtil.loadValueErrsClass(model.className); }
@if(GenJavaUtil.needsLoadValueErrs(model.structural) && errorsClass.equals("LoadValueErrs"))
import configgen.genjava.LoadValueErrs;

@endif
@if(!model.sourceComment.isEmpty())
${model.sourceComment}
@endif
@if(model.cfg.jspecify() && model.structural.foreignKeys().stream().anyMatch(fk -> !(fk.refKey() instanceof RefKey.RefSimple ref && ref.nullable())))
@SuppressWarnings("NotNullFieldNotInitialized")
@endif
@if(model.isImpl)
    public ${model.isSealedInterface?"final ":""}class ${model.className}${model.javaBase.isEmpty() ? "" : " extends " + model.javaBase} implements ${model.nullableInterfaceFullName} {
@else
    public class ${model.className}${model.javaBase.isEmpty() ? "" : " extends " + model.javaBase} {
@endif

    @for(FieldInfo field : model.fields)
        private final ${field.type()} ${field.name()};
    @endfor

    @if(!model.foreignKeys.isEmpty())
        @for(ForeignKeyInfo fk : model.foreignKeys)
            private ${fk.type()} ${fk.name()};
        @endfor
    @endif

@if(!model.isTable)
${"\n"}    public ${model.className}(${model.formalParams()}) {
        @for(FieldInfo field : model.fields)
        this.${field.name()} = ${field.name()};
        @endfor
    }

@elseif(model.isTableAndNeedBuilder)
${"\n"}    ${model.className}(${model.className}Builder b) {
        @for(FieldInfo field : model.fields)
        this.${field.name()} = ${field.needsBuilderValue() ? "java.util.Objects.requireNonNull(b." + field.name() + ", \"" + field.name() + "\")" : "b." + field.name()};
        @endfor
    }

@endif

${model.isTable && !model.isTableAndNeedBuilder ? "\n" : ""}    private ${model.className}(configgen.genjava.ConfigInput input) {
        @for(FieldSchema field : model.structural.fields())
            !{
                String ln = StringUtil.lower1(field.name());
            }
            @if(field.type() instanceof FieldType.SimpleType simpleType)
                this.${ln} = ${model.readValue(simpleType)};
            @elseif(field.type() instanceof FieldType.FList fList)
                {
                    int c = input.readCount(0);
                    if (c == 0) {
                        this.${ln} = java.util.Collections.emptyList();
                    } else {
                        var items = new java.util.ArrayList<${TypeStr.boxType(model.cfg, fList.item())}>(c);
                        for (; c > 0; c--) {
                            items.add(${model.readValue(fList.item())});
                        }
                        this.${ln} = java.util.Collections.unmodifiableList(items);
                    }
                }
            @elseif(field.type() instanceof FieldType.FMap fMap)
                {
                    int c = input.readCount(0);
                    if (c == 0) {
                        this.${ln} = java.util.Collections.emptyMap();
                    } else {
                        var entries = new java.util.LinkedHashMap<${TypeStr.boxType(model.cfg, fMap.key())}, ${TypeStr.boxType(model.cfg, fMap.value())}>(c);
                        for (; c > 0; c--) {
                            var key = ${model.readValue(fMap.key())};
                            var value = ${model.readValue(fMap.value())};
                            if (entries.putIfAbsent(key, value) != null) {
                                input.errors().duplicateKey("${model.structural.name()}.${field.name()}", key);
                            }
                        }
                        this.${ln} = java.util.Collections.unmodifiableMap(entries);
                    }
                }
            @endif
        @endfor
    }

    @if(model.isImpl && model.enumRefTable != null)
    @Override
    public ${model.refType(model.enumRefTable)} type() {
        return ${model.refType(model.enumRefTable)}.${model.enumFieldName(model.structural.name())};
    }

    @endif

    @for(FieldInfo field : model.fields)
        @if(!field.comment().isEmpty())
        /**
        @for(String line : field.comment().split("\n")) * ${line}@endfor
         */
        @endif
        public ${field.type()} get${StringUtil.upper1(field.name())}() {
            return ${field.name()};
        }

    @endfor

    @if(!model.foreignKeys.isEmpty())
        @for(ForeignKeyInfo fk : model.foreignKeys)
            public ${fk.type()} ${StringUtil.lower1(fk.name())}() {
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
        public boolean equals(${model.cfg.nullable("Object")} other) {
            return other instanceof ${model.className};
        }

    @elseif(!model.isTable)
        @Override
        public int hashCode() {
            return ${model.hashCodes()};
        }

        @Override
        public boolean equals(${model.cfg.nullable("Object")} other) {
            if (!(other instanceof ${model.className} o)) {
                return false;
            }
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

    @if(model.hasRef)
        @if(model.nullableInterface!=null)
            @Override
        @endif
        @template.java.GenResolve(cfg = model.cfg, structural = model.structural, errorsClass = errorsClass)
    @endif

    @if(GenJavaUtil.hasExtension(model.structural))
        @template.java.GenCollectExtensions(model = model)
    @endif

    public static ${model.className} _create(configgen.genjava.ConfigInput input) {
        return new ${model.className}(input);
    }

    @if(model.isTable)
            @template.java.GenStructuralClassTablePart(model = model)
    @endif
}
