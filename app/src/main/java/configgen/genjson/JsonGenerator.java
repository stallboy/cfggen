package configgen.genjson;

import configgen.ctx.Context;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.schema.HasMap;
import configgen.util.Logger;
import configgen.value.CfgValue;
import configgen.write.VTableJsonStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static configgen.value.CfgValue.*;

public class JsonGenerator extends GeneratorWithTag {
    private final String tables;
    private final String dst;

    public JsonGenerator(Parameter parameter) {
        super(parameter);
        tables = parameter.get("tables", "");
        dst = parameter.get("dst", ".");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        String[] tableNames = tables.split(";");
        if (tableNames.length == 0) {
            return;
        }
        if (tag != null) {
            Logger.log("gen json with tag=%s, be careful!!!", tag);
        }

        CfgValue cfgValue = ctx.makeValue(tag);

        for (String table : tableNames) {
            VTable vTable = cfgValue.getTable(table);
            if (vTable == null) {
                Logger.log("ignore gen json: table=%s not found!", table);
                continue;
            }

            if (HasMap.hasMap(vTable.schema())) {
                Logger.log("ignore gen json: table=%s has map!", table);
                continue;
            }

            for (Map.Entry<Value, VStruct> e : vTable.primaryKeyMap().entrySet()) {
                Value pk = e.getKey();
                VStruct record = e.getValue();
                VTableJsonStorage.addOrUpdateRecord(record, table, pk.packStr(), Path.of(dst));
            }
        }
    }
}
