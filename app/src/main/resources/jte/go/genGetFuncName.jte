@import configgen.gengo.GenGo
@import configgen.gengo.GoName
@import configgen.schema.KeySchema
@param KeySchema keySchema
@param GoName name
@param boolean refPrimary
!{
    var IdType = GenGo.keyClassName(keySchema);
    var paramVars = GenGo.keyClassName(keySchema);
}
codeGetFuncName = refPrimary ? "Get" :
(fieldCnt > 1 ? "GetBy${IdType}" : "GetBy${paramVars}")
.replace("${IdType}", GenGo.keyClassName(keySchema))
.replace("${paramVars}", paramVars);