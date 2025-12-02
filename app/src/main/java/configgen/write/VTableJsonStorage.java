package configgen.write;

import configgen.gen.Generator;
import configgen.util.CachedFiles;
import configgen.value.CfgValue.VStruct;
import configgen.value.ValueToJson;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;

import static configgen.data.DataUtil.getJsonTableDir;

/**
 * 对json类别的table做add、update、delete
 * 不做任何内存数据结构的修改，只读。
 * 因为可能是多线程调用，对CfgValueStat，对DirectoryStructure的修改留给外层来做
 */
public class VTableJsonStorage {

    public static Path addOrUpdateRecord(@NotNull VStruct record,
                                         @NotNull String table,
                                         @NotNull String id,
                                         @NotNull Path dataDir) throws IOException {
        Path jsonDir = getJsonTableDir(dataDir, table);
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
    public static Path deleteRecord(@NotNull String table,
                                    @NotNull String id,
                                    @NotNull Path dataDir) {
        Path jsonDir = getJsonTableDir(dataDir, table);
        Path recordPath = jsonDir.resolve(id + ".json");
        if (CachedFiles.delete(recordPath.toFile())) {
            return recordPath;
        }
        return null;
    }

}
