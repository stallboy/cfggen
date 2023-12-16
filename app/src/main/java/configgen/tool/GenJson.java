package configgen.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import configgen.gen.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.util.Logger;
import configgen.value.CfgValue;
import configgen.value.ValueFromJson;
import configgen.value.ValueToJson;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

            Path tabPath = dstPath.resolve("_" + vTable.schema().name().replace(".", "_"));
            for (Map.Entry<Value, VStruct> e : vTable.primaryKeyMap().entrySet()) {
                Value pk = e.getKey();
                VStruct value = e.getValue();
                Path recordPath = tabPath.resolve(pk.packStr() + ".json");
                try (OutputStreamWriter writer = createUtf8Writer(recordPath.toFile())) {
                    JSONObject jsonObject = new ValueToJson().toJson(value);
                    String jsonString = JSON.toJSONString(jsonObject, JSONWriter.Feature.PrettyFormat);
                    writer.write(jsonString);
                }
            }
        }
    }
}
