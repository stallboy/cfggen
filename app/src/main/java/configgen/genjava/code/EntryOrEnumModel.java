package configgen.genjava.code;

import configgen.schema.EntryType;
import configgen.schema.FieldType;
import configgen.schema.ForeignKeySchema;
import configgen.schema.TableSchema;
import configgen.value.CfgValue;

import java.util.Map;

public class EntryOrEnumModel {
    // 模板便捷方法（enumFieldName/refType/type）需要；GenEntryOrEnumClass.jte 不直接引用
    private final GenCfg cfg;
    public final String pkg;
    public final NameableName name;
    public final String className;
    public final boolean isEnum;
    public final boolean hasNoIntValue;
    public final Map<String, Integer> enumNameToIntegerValueMap;
    public final Iterable<String> enumNames;
    public final TableSchema table;
    public final boolean isNeedReadData;
    public final String dataNameFullName;
    public final EntryType.EntryBase entryBase;
    public final String codeTopPkg;
    public final String sourceComment; // 类文件顶部来源注释（trailing + 数据文件路径）；无则空串

    public EntryOrEnumModel(GenCfg cfg, CfgValue.VTable vTable, EntryType.EntryBase entryBase, NameableName name,
                           boolean isNeedReadData, NameableName dataName, String sourceComment) {
        this.cfg = cfg;
        this.pkg = name.pkg;
        this.name = name;
        this.codeTopPkg = cfg.codeTopPkg();
        this.className = name.className;
        this.isEnum = entryBase instanceof EntryType.EEnum;
        this.hasNoIntValue = vTable.enumNameToIntegerValueMap() == null;
        this.enumNameToIntegerValueMap = vTable.enumNameToIntegerValueMap();
        this.enumNames = vTable.enumNames();
        this.table = vTable.schema();
        this.isNeedReadData = isNeedReadData;
        this.dataNameFullName = dataName.fullName;
        this.entryBase = entryBase;
        this.sourceComment = sourceComment;
    }

    // 以下为模板渲染的便捷入口：让 .jte 无须再传 cfg 调 Name/TypeStr 的静态方法
    public String enumFieldName(String enumName) {
        return Name.enumFieldName(cfg, enumName);
    }

    public String refType(ForeignKeySchema fk) {
        return Name.refType(cfg, fk);
    }

    public String type(FieldType t) {
        return TypeStr.type(cfg, t);
    }
}
