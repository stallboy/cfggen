@import configgen.gengo.GoCodeGenerator
@import configgen.gengo.GoName
@import configgen.gengo.CfgMgrModel
@import configgen.util.StringUtil
@import configgen.value.CfgValue
@import configgen.value.CfgValue.VTable
@param CfgMgrModel model
!{
    String pkg = model.pkg();
    CfgValue cfgValue = model.cfgValue();
}
package ${pkg}

import "io"

@for (VTable vTable : cfgValue.sortedTables())
    !{
        GoName name = new GoName(vTable.schema());
        var className = StringUtil.lower1(name.className);
        var ClassName = StringUtil.upper1(className);
    }
    var ${className}Mgr *${ClassName}Mgr

    func Get${ClassName}Mgr() *${ClassName}Mgr {
        return ${className}Mgr
    }

@endfor
func Init(reader io.Reader) {
    myStream := &Stream{reader: reader}
    for {
        cfgName := myStream.ReadString()
        if cfgName == "" {
            break
        }
        switch cfgName {
        @for (VTable vTable : cfgValue.sortedTables())
        !{
            GoName name = new GoName(vTable.schema());
            var className = StringUtil.lower1(name.className);
            var ClassName = StringUtil.upper1(className);
            var ClassReadName = name.pkgName;
        }
        case "${ClassReadName}":
            ${className}Mgr = &${ClassName}Mgr{}
            ${className}Mgr.Init(myStream)
        @endfor
        }
    }
}
