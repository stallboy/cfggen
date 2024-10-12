package configgen.tool;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.util.CachedIndentPrinter;
import configgen.util.Logger;
import configgen.value.CfgValue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenTs extends Generator {
    private final List<String> tables = new ArrayList<>();
    private final Path dstPath;
    private final String encoding;

    public GenTs(Parameter parameter) {
        super(parameter);
        String tablesStr = parameter.get("tables", "");
        String dstDir = parameter.get("dst", ".");
        encoding = parameter.get("encoding", "UTF-8");
        dstPath = Path.of(dstDir);

        String[] split = tablesStr.split(";");
        tables.addAll(Arrays.asList(split));
        if (tag != null) {
            Logger.log("gen ts with tag=%s, be careful!!!", tag);
        }
    }

    @Override
    public void generate(Context ctx) throws IOException {
        if (tables.isEmpty()) {
            return;
        }
        CfgValue cfgValue = ctx.makeValue(tag);

        for (String table : tables) {
            CfgValue.VTable vTable = cfgValue.vTableMap().get(table);
            if (vTable == null) {
                Logger.log("ignore gen ts: table=%s not found!", table);
                continue;
            }
            try (CachedIndentPrinter ps = createCode(dstPath.resolve(table + ".ts").toFile(), encoding)) {
                String generate = new SchemaToTs(cfgValue, vTable.schema()).generate();
                ps.println(generate);
            }
        }
    }
}