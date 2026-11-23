package configgen.write;

import org.jspecify.annotations.Nullable;

import configgen.data.Source.DFile;
import configgen.layout.DirectoryStructure;
import configgen.layout.JsonLayout;
import configgen.util.FileUtil;
import configgen.util.CachedFiles;
import configgen.value.CfgValue.VStruct;
import configgen.value.ValueToJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    public static Path addOrUpdateRecord(VStruct record,
                                         String table,
                                         String id,
                                         Path dataDir,
                                         DirectoryStructure directoryStructure) throws IOException {
        Path relativePath = recordPath(table, id, dataDir, directoryStructure);
        FileUtil.replaceIfChanged(dataDir.resolve(relativePath), recordBytes(record));
        return relativePath;
    }

    static Path saveEditedRecord(VStruct record, @Nullable VStruct oldRecord, String table, String id,
                                 Path dataDir, DirectoryStructure directoryStructure) throws IOException {
        Path relativePath = oldRecord == null
                ? recordPath(table, id, dataDir, directoryStructure)
                : sourcePath(oldRecord);
        Path target = dataDir.resolve(relativePath);
        if (oldRecord == null && Files.exists(target, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("JSON record file already exists: " + target);
        }
        FileUtil.replaceIfChanged(target, recordBytes(record));
        return relativePath;
    }

    static Path deleteRecord(VStruct record, Path dataDir) throws IOException {
        Path relativePath = sourcePath(record);
        Files.delete(dataDir.resolve(relativePath));
        return relativePath;
    }

    private static Path sourcePath(VStruct record) {
        return Path.of(((DFile) record.source()).fileName());
    }

    private static Path recordPath(String table, String id, Path dataDir,
                                   DirectoryStructure directoryStructure) throws IOException {
        Path directory = relativeTableDirectory(table, dataDir, directoryStructure);
        return directory.resolve(JsonLayout.jsonRecordFileName(id, directory.getFileSystem()));
    }

    private static Path relativeTableDirectory(String table, Path dataDir, DirectoryStructure directoryStructure) {
        Path existingDir = directoryStructure.getJsonTableDir(table);
        return existingDir != null ? existingDir : JsonLayout.newTableDirectory(table, dataDir);
    }

    public static void writeRecord(VStruct record, Path path, CachedFiles cachedFiles) throws IOException {
        cachedFiles.writeFileReplacement(path, recordBytes(record));
    }

    private static byte[] recordBytes(VStruct record) {
        return ValueToJson.toJsonStr(record).getBytes(StandardCharsets.UTF_8);
    }
}
