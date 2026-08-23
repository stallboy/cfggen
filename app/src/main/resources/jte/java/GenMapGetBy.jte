@import configgen.genjava.code.*
@import configgen.genjava.code.MethodStr
@import configgen.schema.*
@param KeySchema keySchema
@param NameableName name
@param boolean isPrimaryKey
    !{
        String methodName = Name.GetByKeyFunctionNameInConfigMgr(name.cfg, keySchema, isPrimaryKey, name.nameable);
        String getByName = Name.GetByKeyFunctionName(keySchema, isPrimaryKey);
    }
public static ${name.className} ${getByName}(${MethodStr.formalParams(name.cfg, keySchema.fieldSchemas())}) {
        ${name.cfg.codeTopPkg()}.ConfigMgr mgr = ${name.cfg.codeTopPkg()}.ConfigMgr.getMgr();
        return mgr.${methodName}(${MethodStr.actualParamsKeyRaw(keySchema, "")});
    }
