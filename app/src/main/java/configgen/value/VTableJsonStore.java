package configgen.value;

import configgen.gen.Generator;
import configgen.schema.TableSchema;
import configgen.util.CachedFiles;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;

import static configgen.data.DataUtil.getJsonTableDir;

public class VTableJsonStore {

    public static Path addOrUpdateRecordStore(CfgValue.VStruct record,
                                              TableSchema tableSchema,
                                              String id,
                                              Path dataDir,
                                              ValueStat valueStat) throws IOException {
        Path jsonDir = getJsonTableDir(dataDir, tableSchema.name());
        Path recordPath = jsonDir.resolve(id + ".json");
        try (OutputStreamWriter writer = Generator.createUtf8Writer(recordPath.toFile())) {
            String jsonString = ValueToJson.toJsonStr(record);
            writer.write(jsonString);
            valueStat.addLastModified(tableSchema.name(), id, System.currentTimeMillis());
            return recordPath;
        }
    }

    public static boolean deleteRecordStore(TableSchema tableSchema, String id, Path dataDir, ValueStat valueStat) {
        Path jsonDir = getJsonTableDir(dataDir, tableSchema.name());
        Path recordPath = jsonDir.resolve(id + ".json");
        valueStat.removeLastModified(tableSchema.name(), id);
        return CachedFiles.delete(recordPath.toFile());
    }

}
