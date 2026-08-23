@import configgen.genjava.code.Name
@import configgen.genjava.code.TypeStr
@import configgen.genjava.code.MethodStr
@import configgen.schema.*
@import configgen.util.StringUtil
@param configgen.genjava.code.GenCfg cfg
@param KeySchema keySchema
public static class ${Name.keyClassName(cfg, keySchema)} {
        @for(FieldSchema f : keySchema.fieldSchemas())
            private final ${TypeStr.type(cfg, f.type())} ${StringUtil.lower1(f.name())};
        @endfor


        public ${Name.keyClassName(cfg, keySchema)}(${MethodStr.formalParams(cfg, keySchema.fieldSchemas())}) {
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
            if (!(other instanceof ${Name.keyClassName(cfg, keySchema)}))
                return false;
            ${Name.keyClassName(cfg, keySchema)} o = (${Name.keyClassName(cfg, keySchema)}) other;
            return ${MethodStr.equals(keySchema.fieldSchemas())};
        }
    }
