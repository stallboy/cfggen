package configgen.tool;

import configgen.gen.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.util.Logger;
import configgen.value.CfgValue;
import configgen.value.VTableJsonParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;

public class GenJson extends Generator {
    private final List<String> tables = new ArrayList<>();
    private final Path dstPath;

    public GenJson(Parameter parameter) {
        super(parameter);
        String tablesStr = parameter.get("tables", "");
        String dstDir = parameter.get("dst", ".");
        dstPath = Path.of(dstDir);

        String[] split = tablesStr.split(";");
        tables.addAll(Arrays.asList(split));
        if (tag != null) {
            Logger.log("gen json with tag=%s, be careful!!!", tag);
        }
    }

    @Override
    public void generate(Context ctx) throws IOException {
        if (tables.isEmpty()) {
            return;
        }
        CfgValue cfgValue = ctx.makeValue(tag);

        for (String table : tables) {
            VTable vTable = cfgValue.vTableMap().get(table);
            if (vTable == null) {
                Logger.log("gen json table=%s not found!", table);
                continue;
            }

            for (Map.Entry<Value, VStruct> e : vTable.primaryKeyMap().entrySet()) {
                Value pk = e.getKey();
                VStruct record = e.getValue();
                VTableJsonParser.addOrUpdateRecordStore(record, vTable.schema(), pk.packStr(), dstPath);
            }
        }
    }
}
