package configgen.genbyai;

import configgen.ctx.Context;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.util.CachedIndentPrinter;
import configgen.util.Logger;
import configgen.value.CfgValue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TsSchemaGenerator extends GeneratorWithTag {
    private final String table;
    private final List<String> refTables;
    private final Path dstPath;
    private final String encoding;

    public TsSchemaGenerator(Parameter parameter) {
        super(parameter);
        String tableStr = parameter.get("table", "");
        String dstDir = parameter.get("dst", ".");
        encoding = parameter.get("encoding", "UTF-8");
        dstPath = Path.of(dstDir);
        String[] split = tableStr.split(";");
        table = split.length > 0 ? split[0] : "";
        refTables = new ArrayList<>();
        if (split.length > 1) {
            refTables.addAll(Arrays.asList(split).subList(1, split.length));
        }
    }

    @Override
    public void generate(Context ctx) throws IOException {
        if (table.isEmpty()) {
            return;
        }
        if (tag != null) {
            Logger.log("gen ts with tag=%s, be careful!!!", tag);
        }

        CfgValue cfgValue = ctx.makeValue(tag);

        CfgValue.VTable vTable = cfgValue.vTableMap().get(table);
        if (vTable == null) {
            Logger.log("ignore gen ts: table=%s not found!", table);
            return;
        }
        try (CachedIndentPrinter ps = new CachedIndentPrinter(dstPath.resolve(table + ".ts"), encoding)) {
            String generate = new SchemaToTs(cfgValue, vTable.schema(), refTables, false).generate();
            ps.println(generate);
        }
    }
}