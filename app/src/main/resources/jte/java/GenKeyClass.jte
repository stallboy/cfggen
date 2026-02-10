@import configgen.gen.Generator
@import configgen.genjava.code.Name
@import configgen.genjava.code.TypeStr
@import configgen.genjava.code.MethodStr
@import configgen.schema.*
@import configgen.util.StringUtil
@param KeySchema keySchema
public static class ${Name.keyClassName(keySchema)} {
        @for(FieldSchema f : keySchema.fieldSchemas())
            private final ${TypeStr.type(f.type())} ${StringUtil.lower1(f.name())};
        @endfor


        public ${Name.keyClassName(keySchema)}(${MethodStr.formalParams(keySchema.fieldSchemas())}) {
            @for(FieldSchema f : keySchema.fieldSchemas())
                this.${StringUtil.lower1(f.name())} = ${StringUtil.lower1(f.name())};
            @endfor
        }

        @Override
        public int hashCode() {
            return ${MethodStr.hashCodes(keySchema.fieldSchemas())};
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ${Name.keyClassName(keySchema)}))
                return false;
            ${Name.keyClassName(keySchema)} o = (${Name.keyClassName(keySchema)}) other;
            return ${MethodStr.equals(keySchema.fieldSchemas())};
        }
    }
