package configgen.tool;

import configgen.gen.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.schema.TableSchema;
import configgen.value.CfgValue;
import configgen.value.ForeachVStruct;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

import static configgen.value.CfgValue.VStruct;
import static configgen.value.CfgValue.VTable;
import static configgen.value.ForeachVStruct.VStructVisitor;

public class GenAllRefValues extends Generator {
    private final String ref;
    private final Set<String> ignores = new HashSet<>();
    private final String out;

    private final boolean genSrc;

    private String curTableName;

    private class RefCell {
        String refName;
        HashSet<String> tableNames;

        public RefCell(String _refName) {
            refName = _refName;
            tableNames = new HashSet<>();
        }
    }

    public GenAllRefValues(Parameter parameter) {
        super(parameter);
        ref = parameter.get("ref", "asset.assets", "目标表名");
        String ignoreStr = parameter.get("ignores", null, "忽略这些表，如果不设置则自动忽略ref");
        if (ignoreStr != null) {
            if (!ignoreStr.isEmpty()){
                Collections.addAll(ignores, ignoreStr.split(","));
            }
        }else{
            ignores.add(ref);
        }
        out = parameter.get("out", "refassets.csv", "生成文件");
        genSrc = parameter.get("gensrc", "false", "生成引用来源").equals("true");
        parameter.end();
    }

    @Override
    public void generate(Context ctx) throws IOException {
        Map<String, RefCell> allrefs = new HashMap<>();
        CfgValue cfgValue = ctx.makeValue(tag);
        TableSchema refTable = cfgValue.schema().findTable(ref);
        if (refTable == null) {
            System.out.println("ref " + ref + " not a table");
            return;
        }

        VStructVisitor visitor = (VStruct vStruct, VTable fromVTable) -> {
//            boolean has = false;
//            for (SRef sr : value.getType().getConstraint().references) {
//                if (sr.refTable == refTable) {
//                    has = true;
//                    break;
//                }
//            }
//            if (has) {
//                var refName = value.getRawString();
//                if (value.isCellEmpty())
//                    return;
//                allrefs.putIfAbsent(refName, new RefCell(refName));
//                allrefs.get(refName).tableNames.add(curTableName);
//            }

        };
        ForeachVStruct foreach = new ForeachVStruct(cfgValue);

        for (VTable vTable : cfgValue.tables()) {
            if (!ignores.contains(vTable.name())) {
                foreach.forEachVTable(visitor, vTable);
            }
        }


        try (OutputStreamWriter writer = createUtf8Writer(new File(out))) {
            if (genSrc) {
                StringBuilder content = new StringBuilder();
                content.append("refAsset,table count,tables\r\n");
                var sorted = new ArrayList<>(allrefs.values());
                sorted.sort(Comparator.comparing(o -> o.refName));
                sorted.forEach(refCell -> {
                    StringBuilder line = new StringBuilder();
                    line.append(refCell.refName);
                    line.append(',');
                    int cnt = refCell.tableNames.size();
                    line.append(cnt);
                    line.append(',');
                    var idx = 0;
                    for (String tableName : refCell.tableNames) {
                        line.append(tableName);
                        if (++idx == cnt) {
                            line.append("\r\n");
                        } else {
                            line.append(",");
                        }
                    }
                    content.append(line);
                });
                writer.write(content.toString());
            } else
                writer.write(String.join("\r\n", allrefs.keySet()));
        }
    }
}
