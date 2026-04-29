@import configgen.genjava.code.*
@import configgen.genjava.code.MethodStr
@import configgen.schema.*
@import java.util.List
@param KeySchema keySchema
@param NameableName name
@param boolean isPrimaryKey
@param String codeTopPkg
@param List<String> mapsInMgr
@param boolean isSeq
    !{
        String mapName = name.containerPrefix + (isPrimaryKey ? "All" : Name.uniqueKeyMapName(keySchema));
        String keyTypeName = Name.keyClassName(keySchema, name);
        String methodName = Name.GetByKeyFunctionNameInConfigMgr(keySchema, isPrimaryKey, name.nameable);

        if (isSeq) {
            mapsInMgr.add(String.format("public %s[] %s;",name.fullName, mapName));

            mapsInMgr.add(String.format("public %s %s(%s) { return %s >= 0 && %s < %s.length ? %s[%s] : null; }",
                name.fullName, methodName,
                MethodStr.formalParams(keySchema.fieldSchemas()),
                MethodStr.actualParamsKeyRaw(keySchema, ""),
                MethodStr.actualParamsKeyRaw(keySchema, ""),
                mapName, mapName, MethodStr.actualParamsKeyRaw(keySchema, "")));
        } else {
            mapsInMgr.add(String.format("public java.util.Map<%s, %s> %s;",
                keyTypeName, name.fullName, mapName));

            mapsInMgr.add(String.format("public %s %s(%s) { return %s.get(%s); }",
                name.fullName, methodName,
                MethodStr.formalParams(keySchema.fieldSchemas()), mapName, MethodStr.actualParamsKey(keySchema, "", name)));
        }

        String getByName = Name.GetByKeyFunctionName(keySchema, isPrimaryKey);
    }
public static ${name.className} ${getByName}(${MethodStr.formalParams(keySchema.fieldSchemas())}) {
        ${codeTopPkg}.ConfigMgr mgr = ${codeTopPkg}.ConfigMgr.getMgr();
        return mgr.${methodName}(${MethodStr.actualParamsKeyRaw(keySchema, "")});
    }
