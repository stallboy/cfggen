@import configgen.genjava.code.*
@import configgen.genjava.code.MethodStr
@import configgen.schema.*
@import java.util.List
@param KeySchema keySchema
@param NameableName name
@param boolean isPrimaryKey
@param String codeTopPkg
@param List<String> mapsInMgr

!{
    String mapName = name.className + (isPrimaryKey ? "All" : Name.uniqueKeyMapName(keySchema));
    String keyTypeName = Name.keyClassName(keySchema, name);
    var methodName = Name.GetByKeyFunctionNameInConfigMgr(keySchema, isPrimaryKey, name.nameable);
    String getByName = Name.GetByKeyFunctionName(keySchema, isPrimaryKey);

    mapsInMgr.add(String.format("public final java.util.Map<%s, %s> %s = new java.util.LinkedHashMap<>();", 
        keyTypeName, name.fullName, mapName));
    mapsInMgr.add(String.format("public %s %s(%s) { return %s.get(%s); }", 
        name.fullName, methodName,
        MethodStr.formalParams(keySchema.fieldSchemas()), mapName, MethodStr.actualParamsKey(keySchema, "", name)));
}
public static ${name.className} ${getByName}(${MethodStr.formalParams(keySchema.fieldSchemas())}) {
    ${codeTopPkg}.ConfigMgr mgr = ${codeTopPkg}.ConfigMgr.getMgr();
    return mgr.${methodName}(${MethodStr.actualParamsKeyRaw(keySchema, "")});
}
