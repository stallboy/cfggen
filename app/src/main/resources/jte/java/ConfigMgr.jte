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
    private boolean ready;
    private final java.util.Map<String, java.util.Map<?, java.util.List<String>>> origins = new java.util.LinkedHashMap<>();

    public <K> java.util.List<String> explain(String table, K key) {
        var rows = origins.get(table);
        return rows == null ? java.util.List.of() : rows.getOrDefault(key, java.util.List.of());
    }

    void setOrigins(String table, java.util.Map<?, java.util.List<String>> rows) {
        requireBuilding();
        origins.put(table, rows);
    }

    boolean isReady() { return ready; }
    void markReady() { ready = true; }
    private void requireBuilding() { if (ready) throw new IllegalStateException("Configuration is read-only"); }

    public static ConfigMgr getMgr() {
        return mgr;
    }

    public static void setMgr(ConfigMgr newMgr) {
        if (newMgr == null || !newMgr.ready) throw new IllegalStateException("Configuration is not ready");
        mgr = newMgr;
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
    @if(table.entry() instanceof EntryType.EntryBase)
    private ${name.fullName}[] ${name.containerPrefix}Refs;
    public ${name.fullName} _get_${name.containerPrefix}Refs(int index) { return ${name.containerPrefix}Refs[index]; }
    public void _set_${name.containerPrefix}Refs(${name.fullName}[] refs) { requireBuilding(); ${name.containerPrefix}Refs = refs; }

    @endif
    @if(seq)
    private java.util.List<${name.fullName}> ${mapName};
    public java.util.List<${name.fullName}> _get_${mapName}() { return ${mapName}; }
    public void _set_${mapName}(java.util.List<${name.fullName}> rows) { requireBuilding(); ${mapName} = java.util.Collections.unmodifiableList(rows); }

    public ${name.fullName} ${methodName}(${MethodStr.formalParams(name.cfg, primaryKey.fieldSchemas())}) { return ${MethodStr.actualParamsKeyRaw(primaryKey, "")} >= 0 && ${MethodStr.actualParamsKeyRaw(primaryKey, "")} < ${mapName}.size() ? ${mapName}.get(${MethodStr.actualParamsKeyRaw(primaryKey, "")}) : null; }

    @else
    private java.util.Map<${keyTypeName}, ${name.fullName}> ${mapName};
    public java.util.Map<${keyTypeName}, ${name.fullName}> _get_${mapName}() { return ${mapName}; }
    public void _set_${mapName}(java.util.Map<${keyTypeName}, ${name.fullName}> rows) { requireBuilding(); ${mapName} = java.util.Collections.unmodifiableMap(rows); }

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
        private java.util.List<${name.fullName}> ${ukMapName};
        public java.util.List<${name.fullName}> _get_${ukMapName}() { return ${ukMapName}; }
        public void _set_${ukMapName}(java.util.List<${name.fullName}> rows) { requireBuilding(); ${ukMapName} = java.util.Collections.unmodifiableList(rows); }

        public ${name.fullName} ${ukMethodName}(${MethodStr.formalParams(name.cfg, uk.fieldSchemas())}) { return ${MethodStr.actualParamsKeyRaw(uk, "")} >= 0 && ${MethodStr.actualParamsKeyRaw(uk, "")} < ${ukMapName}.size() ? ${ukMapName}.get(${MethodStr.actualParamsKeyRaw(uk, "")}) : null; }

        @else
        private java.util.Map<${ukKeyTypeName}, ${name.fullName}> ${ukMapName};
        public java.util.Map<${ukKeyTypeName}, ${name.fullName}> _get_${ukMapName}() { return ${ukMapName}; }
        public void _set_${ukMapName}(java.util.Map<${ukKeyTypeName}, ${name.fullName}> rows) { requireBuilding(); ${ukMapName} = java.util.Collections.unmodifiableMap(rows); }

        public ${name.fullName} ${ukMethodName}(${MethodStr.formalParams(name.cfg, uk.fieldSchemas())}) { return ${ukMapName}.get(${MethodStr.actualParamsKey(name.cfg, uk, "", name)}); }

        @endif
    @endfor
    !{
        String functionAllName = "all" + Arrays.stream(name.nameable.name().split("\\.")).map(s -> Name.pascalName(name.cfg, s)).collect(Collectors.joining());
    }
    @if(seq)
    public java.util.List<${name.fullName}> ${functionAllName}() { return ${mapName}; }

    @else
    public java.util.Collection<${name.fullName}> ${functionAllName}() { return ${mapName}.values(); }

    @endif
    @endfor
}
