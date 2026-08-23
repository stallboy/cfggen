@import java.util.List
@import java.util.Arrays
@import java.util.stream.Collectors
@import configgen.genjava.code.NameableName
@import configgen.genjava.code.Name
@import configgen.genjava.code.MethodStr
@import configgen.schema.*
@param String pkg
@param List<NameableName> tableDataNames
package ${pkg};

public class ConfigMgr {
    private static volatile ConfigMgr mgr;

    public static ConfigMgr getMgr() {
        return mgr;
    }

    public static void setMgr(ConfigMgr newMgr) {
        mgr = newMgr;
        ConfigMgrLoader.applySetAllRefs(mgr);
    }

    @for(NameableName name : tableDataNames)
    !{
        TableSchema table = (TableSchema) name.nameable;
        KeySchema primaryKey = table.primaryKey();
        String mapName = name.containerPrefix + "All";
        boolean seq = primaryKey.fieldSchemas().size() == 1 && primaryKey.fieldSchemas().getFirst().isSeq();
        String keyTypeName = Name.keyClassName(name.cfg, primaryKey, name);
        String methodName = Name.GetByKeyFunctionNameInConfigMgr(name.cfg, primaryKey, true, name.nameable);
    }
    @if(seq)
    public ${name.fullName}[] ${mapName};

    public ${name.fullName} ${methodName}(${MethodStr.formalParams(name.cfg, primaryKey.fieldSchemas())}) { return ${MethodStr.actualParamsKeyRaw(primaryKey, "")} >= 0 && ${MethodStr.actualParamsKeyRaw(primaryKey, "")} < ${mapName}.length ? ${mapName}[${MethodStr.actualParamsKeyRaw(primaryKey, "")}] : null; }

    @else
    public java.util.Map<${keyTypeName}, ${name.fullName}> ${mapName};

    public ${name.fullName} ${methodName}(${MethodStr.formalParams(name.cfg, primaryKey.fieldSchemas())}) { return ${mapName}.get(${MethodStr.actualParamsKey(name.cfg, primaryKey, "", name)}); }

    @endif
    @for(KeySchema uk : table.uniqueKeys())
        !{
            String ukMapName = name.containerPrefix + Name.uniqueKeyMapName(uk);
            boolean ukSeq = uk.fieldSchemas().size() == 1 && uk.fieldSchemas().getFirst().isSeq();
            String ukKeyTypeName = Name.keyClassName(name.cfg, uk, name);
            String ukMethodName = Name.GetByKeyFunctionNameInConfigMgr(name.cfg, uk, false, name.nameable);
        }
        @if(ukSeq)
        public ${name.fullName}[] ${ukMapName};

        public ${name.fullName} ${ukMethodName}(${MethodStr.formalParams(name.cfg, uk.fieldSchemas())}) { return ${MethodStr.actualParamsKeyRaw(uk, "")} >= 0 && ${MethodStr.actualParamsKeyRaw(uk, "")} < ${ukMapName}.length ? ${ukMapName}[${MethodStr.actualParamsKeyRaw(uk, "")}] : null; }

        @else
        public java.util.Map<${ukKeyTypeName}, ${name.fullName}> ${ukMapName};

        public ${name.fullName} ${ukMethodName}(${MethodStr.formalParams(name.cfg, uk.fieldSchemas())}) { return ${ukMapName}.get(${MethodStr.actualParamsKey(name.cfg, uk, "", name)}); }

        @endif
    @endfor
    !{
        String functionAllName = "all" + Arrays.stream(name.nameable.name().split("\\.")).map(s -> Name.pascalName(name.cfg, s)).collect(Collectors.joining());
    }
    @if(seq)
    public java.util.List<${name.fullName}> ${functionAllName}() { return java.util.Arrays.asList(${mapName}); }

    @else
    public java.util.Collection<${name.fullName}> ${functionAllName}() { return ${mapName}.values(); }

    @endif
    @endfor
    !{
        // 收集所有容器字段名，供 copyFrom 展开成引用拷贝
        List<String> copyFields = new java.util.ArrayList<>();
        for (NameableName n : tableDataNames) {
            TableSchema t = (TableSchema) n.nameable;
            copyFields.add(n.containerPrefix + "All");
            for (KeySchema uk : t.uniqueKeys()) {
                copyFields.add(n.containerPrefix + Name.uniqueKeyMapName(uk));
            }
        }
    }

    public void copyFrom(${pkg}.ConfigMgr src) {
        @for(String f : copyFields)
        this.${f} = src.${f};
        @endfor
    }
}
