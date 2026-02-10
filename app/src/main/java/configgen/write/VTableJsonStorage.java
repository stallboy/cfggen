package configgen.write;

import configgen.data.DataUtil;
import configgen.util.CachedFileOutputStream;
import configgen.value.CfgValue.VStruct;
import configgen.value.ValueToJson;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 对json类别的table做add、update、delete
 * 不做任何内存数据结构的修改，只读。
 * 因为可能是多线程调用，对CfgValueStat，对DirectoryStructure的修改留给外层来做
 */
public class VTableJsonStorage {

    /**
     * @return relative path of the record file
     */
    public static Path addOrUpdateRecord(@NotNull VStruct record,
                                         @NotNull String table,
                                         @NotNull String id,
                                         @NotNull Path dataDir) throws IOException {

        String jsonDirName = DataUtil.getJsonTableDirName(table);
        Path relativePath = Path.of(jsonDirName).resolve(id + ".json");

        Path recordPath = dataDir.resolve(relativePath);
        try (var writer = CachedFileOutputStream.createUtf8Writer(recordPath)) {
            String jsonString = ValueToJson.toJsonStr(record);
            writer.write(jsonString);
            return relativePath;
        }
    }

    /**
     * @return relative path of the record file
     */
    public static Path deleteRecord(@NotNull String table,
                                    @NotNull String id,
                                    @NotNull Path dataDir) throws IOException {
        String jsonDirName = DataUtil.getJsonTableDirName(table);
        Path relativePath = Path.of(jsonDirName).resolve(id + ".json");
        Path recordPath = dataDir.resolve(relativePath);
        Files.delete(recordPath);
        return relativePath;
    }

}
