package configgen.genjava.code;

import configgen.genjava.GenJavaUtil;
import configgen.value.CfgValue;

import java.util.ArrayList;
import java.util.List;

import static configgen.value.CfgValue.VTable;

public class ConfigMgrLoaderModel {
    public final String pkg;
    public final List<TableInfo> tables;
    public final List<String> setAllRefs_FullClassNames;

    public record TableInfo(String name,
                            String fullName) {
    }

    public ConfigMgrLoaderModel(CfgValue cfgValue, List<String> setAllRefsInMgrLoader) {
        this.pkg = Name.codeTopPkg;
        this.setAllRefs_FullClassNames = setAllRefsInMgrLoader;

        tables = new ArrayList<>(cfgValue.vTableMap().size());
        for (VTable vTable : cfgValue.tables()) {
            if (!GenJavaUtil.isEnumAndHasOnlyPrimaryKeyAndEnumStr(vTable.schema())) {
                tables.add(new TableInfo(vTable.name(), Name.tableDataFullName(vTable.schema())));
            }
        }
    }
}