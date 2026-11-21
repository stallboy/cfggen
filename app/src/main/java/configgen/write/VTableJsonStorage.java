package configgen.write;

import org.jspecify.annotations.Nullable;

import configgen.ctx.DirectoryStructure;
import configgen.data.DataUtil;
import configgen.data.Source.DFile;
import configgen.util.FileNameUtil;
import configgen.util.FileUtil;
import configgen.util.CachedFiles;
import configgen.value.CfgValue.VStruct;
import configgen.value.ValueToJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

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
        return directory.resolve(DataUtil.jsonRecordFileName(id, directory.getFileSystem()));
    }

    private static Path relativeTableDirectory(String table, Path dataDir, DirectoryStructure directoryStructure) {
        Path existingDir = directoryStructure.getJsonTableDir(table);
        return existingDir != null ? existingDir : resolveJsonDirRelativePath(table, dataDir);
    }

    public static void writeRecord(VStruct record, Path path, CachedFiles cachedFiles) throws IOException {
        cachedFiles.writeFileReplacement(path, recordBytes(record));
    }

    private static byte[] recordBytes(VStruct record) {
        return ValueToJson.toJsonStr(record).getBytes(StandardCharsets.UTF_8);
    }

    /// 根据表名和数据目录，解析 JSON 表目录的相对路径。
    /// 优先使用嵌套路径（如 buff/_skill 或 a/b/_c），回退到旧格式（如 _buff_skill）。
    /// 递归查找模块目录链：表名 a.b.c → 找 a/ 目录 → 在其中找 b/ → 拼接 _c
    static Path resolveJsonDirRelativePath(String table, Path dataDir) {
        int lastDotIdx = table.lastIndexOf('.');
        if (lastDotIdx >= 0) {
            String[] moduleParts = table.substring(0, lastDotIdx).split("\\.");
            String subPart = table.substring(lastDotIdx + 1);

            // 递归查找模块目录链
            Path currentDir = dataDir;
            Path relativePath = Path.of("");
            boolean found = true;

            for (String modulePart : moduleParts) {
                String matchedDirName = findModuleDirName(currentDir, modulePart);
                if (matchedDirName == null) {
                    found = false;
                    break;
                }
                relativePath = relativePath.resolve(matchedDirName);
                currentDir = dataDir.resolve(relativePath);
            }

            if (found) {
                return relativePath.resolve("_" + subPart);
            }
        }

        // 回退旧格式：_module_sub
        return Path.of(DataUtil.getJsonTableDirName(table));
    }

    /// 在目录中查找 codeName 匹配的子目录的实际目录名
    private static @Nullable String findModuleDirName(Path dir, String modulePart) {
        try (Stream<Path> paths = Files.list(dir)) {
            for (Path p : paths.toList()) {
                if (!Files.isDirectory(p)) {
                    continue;
                }
                String codeName = FileNameUtil.getCodeName(p.getFileName().toString());
                if (modulePart.equals(codeName)) {
                    return p.getFileName().toString();
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

}
