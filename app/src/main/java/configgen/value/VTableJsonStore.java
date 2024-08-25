package configgen.value;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import configgen.gen.Generator;
import configgen.schema.TableSchema;
import configgen.util.CachedFiles;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;

public class VTableJsonStore {

    public static Path addOrUpdateRecordStore(CfgValue.VStruct record, TableSchema tableSchema, String id, Path dataDir) throws IOException {
        Path jsonDir = getJsonTableDir(tableSchema, dataDir);
        Path recordPath = jsonDir.resolve(id + ".json");
        try (OutputStreamWriter writer = Generator.createUtf8Writer(recordPath.toFile())) {
            ValueToJson toJson = new ValueToJson();
            toJson.setSaveDefault(false);
            JSONObject jsonObject = toJson.toJson(record);
            String jsonString = JSON.toJSONString(jsonObject, JSONWriter.Feature.PrettyFormat);
            writer.write(jsonString);
            return recordPath;
        }
    }

    public static boolean deleteRecordStore(TableSchema tableSchema, String id, Path dataDir) {
        Path jsonDir = getJsonTableDir(tableSchema, dataDir);
        Path recordPath = jsonDir.resolve(id + ".json");
        return CachedFiles.delete(recordPath.toFile());
    }

    static Path getJsonTableDir(TableSchema tableSchema, Path dataDir) {
        return dataDir.resolve(getJsonTableDirName(tableSchema));
    }

    static String getJsonTableDirName(TableSchema tableSchema) {
        return "_" + tableSchema.name().replace(".", "_");
    }

}
