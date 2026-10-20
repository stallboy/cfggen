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
    var ClassReadName = name.pkgName;
}
var ${className}Mgr *${ClassName}Mgr

func Get${ClassName}Mgr() *${ClassName}Mgr {
    return ${className}Mgr
}

@endfor
func Init(reader io.Reader) *Stream {
    data, err := io.ReadAll(reader)
    if err != nil { panic(err) }
    return InitPackage(ReadPackage(data))
}

// InitPackage reads a complete versioned package. Mod composition remains a Java/C# API.
func InitPackage(pack *ConfigPackage) *Stream {
    pack.Name()
    if pack.metadata["requires"] != "" { panic("this loader supports standalone packages; Mod composition requires Java/C#") }
    @for(VTable vTable : cfgValue.sortedTables())
        if _, exists := pack.blocks[".${vTable.name()}"]; !exists { panic("missing table in standalone package: ${vTable.name()}") }
    @endfor
    return initTables(pack.OpenData(${model.languageArguments()}))
}

func initTables(stream *Stream) *Stream {
    tables := stream.ReadTables()
    // 4. 处理表数据
    for _, table := range tables {
        switch table.Name {
        @for (VTable vTable : cfgValue.sortedTables())
        !{
            GoName name = new GoName(vTable.schema());
            var className = StringUtil.lower1(name.className);
            var ClassName = StringUtil.upper1(className);
            var ClassReadName = name.pkgName;
        }
        case "${ClassReadName}":
            ${className}Mgr = &${ClassName}Mgr{}
            ${className}Mgr.Init(table.Input)
            table.Input.RequireEnd()
        @endfor
        default:
        }
    }

    return stream
}
