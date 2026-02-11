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
    stream := &Stream{reader: reader}

    // 1. 跳过 Schema（如果有）
    schemaLength := stream.ReadInt32()
    if schemaLength > 0 {
        stream.SkipBytes(int(schemaLength))
    }

    // 2. 读取 StringPool
    stream.ReadStringPool()

    // 3. 读取 LangTextPool
    stream.ReadLangTextPool()

    // 4. 处理表数据
    tableCount := stream.ReadSize()
    for i := 0; i < tableCount; i++ {
        tableName := stream.ReadString()
        tableSize := stream.ReadSize()
        switch tableName {
        @for (VTable vTable : cfgValue.sortedTables())
        !{
            GoName name = new GoName(vTable.schema());
            var className = StringUtil.lower1(name.className);
            var ClassName = StringUtil.upper1(className);
            var ClassReadName = name.pkgName;
        }
        case "${ClassReadName}":
            ${className}Mgr = &${ClassName}Mgr{}
            ${className}Mgr.Init(stream)
        @endfor
        default:
            stream.SkipBytes(tableSize)
        }
    }

    return stream
}
