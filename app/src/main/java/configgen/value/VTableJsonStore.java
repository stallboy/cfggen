package configgen.value;

import configgen.gen.Generator;
import configgen.schema.TableSchema;
import configgen.util.CachedFiles;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;

import static configgen.data.DataUtil.getJsonTableDir;

/**
 * 不做任何内存数据结构的修改，只读。
 * 因为可能是多线程调用，对CfgValueStat，对DirectoryStructure的修改留给外层来做
 */
public class VTableJsonStore {

    public static Path addOrUpdateRecordStore(CfgValue.VStruct record,
                                              TableSchema tableSchema,
                                              String id,
                                              Path dataDir) throws IOException {
        Path jsonDir = getJsonTableDir(dataDir, tableSchema.name());
        Path recordPath = jsonDir.resolve(id + ".json");
        try (OutputStreamWriter writer = Generator.createUtf8Writer(recordPath.toFile())) {
            String jsonString = ValueToJson.toJsonStr(record);
            writer.write(jsonString);
            return recordPath;
        }
    }

    /**
     * @return 如果为null，表示删除失败，否则表示成功，返回路径
     */
    public static Path deleteRecordStore(TableSchema tableSchema, String id, Path dataDir) {
        Path jsonDir = getJsonTableDir(dataDir, tableSchema.name());
        Path recordPath = jsonDir.resolve(id + ".json");
        if (CachedFiles.delete(recordPath.toFile())){
            return recordPath;
        }
        return null;
    }

}
