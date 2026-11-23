package configgen.layout;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public final class JsonLayout {
    static final String RECORD_SUFFIX = ".json";

    private JsonLayout() {
    }

    /// 根据表名和数据目录，解析 JSON 表目录的相对路径。
    /// 优先使用嵌套路径（如 buff/_skill 或 a/b/_c），回退到旧格式（如 _buff_skill）。
    /// 递归查找模块目录链：表名 a.b.c → 找 a/ 目录 → 在其中找 b/ → 拼接 _c
    public static Path newTableDirectory(String table, Path dataDir) {
        int lastDot = table.lastIndexOf('.');
        if (lastDot >= 0) {
            Path current = dataDir;
            Path relative = Path.of("");
            for (String module : table.substring(0, lastDot).split("\\.")) {
                Path matched = findModuleDirectory(current, module);
                if (matched == null) {
                    return Path.of(flatTableDirectoryName(table));
                }
                current = matched;
                relative = relative.resolve(matched.getFileName().toString());
            }
            return relative.resolve(tableDirectoryName(table.substring(lastDot + 1)));
        }
        return Path.of(flatTableDirectoryName(table));
    }

    private static @Nullable Path findModuleDirectory(Path parent, String module) {
        try {
            return FileNameUtil.findModuleDirectory(parent, module);
        } catch (IOException ignored) {
            return null;
        }
    }

    /// Canonical table directory within a JSON export.
    public static Path exportTableDirectory(String table) {
        Path name = Path.of(table.replace('.', '/'));
        return name.resolveSibling(tableDirectoryName(name.getFileName().toString()));
    }

    private static String tableDirectoryName(String name) {
        return "_" + name;
    }

    static boolean isRecordFileName(String fileName) {
        return fileName.endsWith(RECORD_SUFFIX);
    }

    static String recordId(String fileName) {
        return fileName.substring(0, fileName.length() - RECORD_SUFFIX.length());
    }

    private static String flatTableDirectoryName(String tableName) {
        return tableDirectoryName(tableName.replace(".", "_"));
    }

    public static Path jsonRecordFileName(String id, FileSystem fileSystem) throws IOException {
        if (id.contains("/") || id.contains("\\") || id.contains("..")) {
            throw new IOException("invalid record id (path separator or '..' not allowed): " + id);
        }
        try {
            Path fileName = fileSystem.getPath(id + RECORD_SUFFIX);
            if (fileName.getRoot() != null) {
                throw new IOException("Invalid JSON record filename for id: " + id);
            }
            return fileName;
        } catch (InvalidPathException e) {
            throw new IOException("Invalid JSON record filename for id: " + id, e);
        }
    }

    /// 从 "_" 开头的 JSON 目录名提取子表名。
    /// "_buff" → "buff"；非 "_" 开头、_后不是英文字母、或含中文返回 null。
    static @Nullable String tableNameForDirectory(String modulePrefix, String dirName) {
        String sub = tableDirectorySuffix(dirName);
        if (sub == null) {
            return null;
        }
        return modulePrefix.isEmpty() ? sub.replace('_', '.') : modulePrefix + sub;
    }

    private static @Nullable String tableDirectorySuffix(String dirName) {
        if (!dirName.startsWith("_")) {
            return null;
        }
        String sub = dirName.substring(1);
        if (!FileNameUtil.startsWithAsciiLetter(sub) || FileNameUtil.findFirstHanIndex(sub) >= 0) {
            return null;
        }
        return sub;
    }

    public static boolean isTableDirForJson(String dirName) {
        return tableDirectorySuffix(dirName) != null;
    }
}
