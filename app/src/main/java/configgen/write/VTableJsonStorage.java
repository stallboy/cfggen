package configgen.write;

import configgen.data.DataUtil;
import configgen.util.CachedFileOutputStream;
import configgen.value.CfgValue.VStruct;
import configgen.value.ValueToJson;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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
    public static Path addOrUpdateRecord(@NotNull VStruct record,
                                         @NotNull String table,
                                         @NotNull String id,
                                         @NotNull Path dataDir) throws IOException {

        Path jsonDirRelPath = resolveJsonDirRelativePath(table, dataDir);
        Path relativePath = jsonDirRelPath.resolve(id + ".json");

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
        Path jsonDirRelPath = resolveJsonDirRelativePath(table, dataDir);
        Path relativePath = jsonDirRelPath.resolve(id + ".json");
        Path recordPath = dataDir.resolve(relativePath);
        Files.delete(recordPath);
        return relativePath;
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
    private static String findModuleDirName(Path dir, String modulePart) {
        try (Stream<Path> paths = Files.list(dir)) {
            for (Path p : paths.toList()) {
                if (!Files.isDirectory(p)) {
                    continue;
                }
                String codeName = DataUtil.getCodeName(p.getFileName().toString());
                if (modulePart.equals(codeName)) {
                    return p.getFileName().toString();
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

}
