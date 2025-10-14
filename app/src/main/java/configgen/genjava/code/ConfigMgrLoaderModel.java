package configgen.genjava.code;

import configgen.genjava.GenJavaUtil;
import configgen.value.CfgValue;

import java.util.ArrayList;
import java.util.List;

import static configgen.value.CfgValue.VTable;

public class ConfigMgrLoaderModel {
    public final String pkg;
    public final List<TableInfo> tables;

    public record TableInfo(String name,
                            String fullName) {
    }

    public ConfigMgrLoaderModel(CfgValue cfgValue) {
        this.pkg = Name.codeTopPkg;

        tables = new ArrayList<>(cfgValue.vTableMap().size());
        for (VTable vTable : cfgValue.tables()) {
            if (!GenJavaUtil.isEnumAndHasOnlyPrimaryKeyAndEnumStr(vTable.schema())) {
                tables.add(new TableInfo(vTable.name(), Name.tableDataFullName(vTable.schema())));
            }
        }
    }
}