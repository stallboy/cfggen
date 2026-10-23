package configgen.genjava.code;

import configgen.genjava.GenJavaUtil;
import configgen.schema.HasRef;
import configgen.schema.TableSchema;
import configgen.util.StringUtil;
import configgen.value.CfgValue;

import java.util.ArrayList;
import java.util.List;

import static configgen.value.CfgValue.VTable;

public class ConfigMgrLoaderModel {
    public final String pkg;
    public final List<TableInfo> tables;
    public final List<String> setAllRefs_FullClassNames;
    public final String languageLiterals;
    public final List<String> extensionRows = new ArrayList<>();
    public final String relationBinding;

    public record TableInfo(String name,
                            String fullName,
                            String key,
                            String index,
                            String resolve) {
    }

    public ConfigMgrLoaderModel(GenCfg cfg, CfgValue cfgValue, List<String> setAllRefsInMgrLoader, List<String> languages) {
        this.pkg = cfg.codeTopPkg();
        this.relationBinding = cfgValue.schema().references().hasRelations() ? GenRelations.generate(cfg, cfgValue.schema()) : "";
        this.setAllRefs_FullClassNames = setAllRefsInMgrLoader;
        this.languageLiterals = languages.stream().map(s -> "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(java.util.stream.Collectors.joining(", "));

        tables = new ArrayList<>(cfgValue.vTableMap().size());
        for (VTable vTable : cfgValue.tables()) {
            if (GenJavaUtil.needsTableData(vTable.schema())) {
                TableSchema table = vTable.schema();
                String fullName = Name.tableDataFullName(cfg, table);
                String container = new NameableName(cfg, table).containerPrefix + "All";
                String key = table.primaryKey().fields().stream().map(f -> "row.get" + StringUtil.upper1(f) + "()")
                        .collect(java.util.stream.Collectors.joining(", "));
                key = table.primaryKey().fields().size() == 1
                        ? fullName + "::get" + StringUtil.upper1(table.primaryKey().fields().getFirst())
                        : "row -> new " + fullName + "." + Name.keyClassName(cfg, table.primaryKey()) + "(" + key + ")";
                tables.add(new TableInfo(vTable.name(), fullName, key,
                        GenJavaUtil.needsIndex(table) ? "rows -> " + fullName + "._index(mgr, rows, errors)" : "mgr::_set_" + container,
                        HasRef.hasRef(table) ? "row -> row._resolve(mgr, errors)" : "null"));
                if (GenJavaUtil.hasExtension(table)) {
                    boolean seq = table.primaryKey().fieldSchemas().size() == 1 && table.primaryKey().fieldSchemas().getFirst().isSeq();
                    extensionRows.add("mgr._get_" + container + "()" + (seq ? "" : ".values()"));
                }
            }
        }
    }
}
