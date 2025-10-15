package configgen.genjava.code;

import configgen.schema.EntryType;
import configgen.schema.TableSchema;
import configgen.value.CfgValue;

import java.util.Map;

public class EntryOrEnumModel {
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

    public EntryOrEnumModel(CfgValue.VTable vTable, EntryType.EntryBase entryBase, NameableName name,
                           boolean isNeedReadData, NameableName dataName) {
        this.pkg = name.pkg;
        this.name = name;
        this.codeTopPkg = Name.codeTopPkg;
        this.className = name.className;
        this.isEnum = entryBase instanceof EntryType.EEnum;
        this.hasNoIntValue = vTable.enumNameToIntegerValueMap() == null;
        this.enumNameToIntegerValueMap = vTable.enumNameToIntegerValueMap();
        this.enumNames = vTable.enumNames();
        this.table = vTable.schema();
        this.isNeedReadData = isNeedReadData;
        this.dataNameFullName = dataName.fullName;
        this.entryBase = entryBase;
    }
}