package configgen.data;

import org.jspecify.annotations.Nullable;

import configgen.util.FileNameUtil;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static configgen.data.DataUtil.FileFmt.*;

public class DataUtil {

    public enum FileFmt {
        TXT_AS_TSV,
        CSV,
        EXCEL,
        CFG,
        JSON,
    }


    public static boolean isFileIgnored(Path path) {
        return path.toFile().isHidden() || path.getFileName().toString().startsWith("~");
    }

    public static @Nullable FileFmt getFileFormat(Path path) {
        String fileName = path.getFileName().toString();
        String ext = "";
        int i = fileName.lastIndexOf('.');
        if (i >= 0) {
            ext = fileName.substring(i + 1).toLowerCase(Locale.ROOT);
        }

        return switch (ext) {
            case "txt" -> TXT_AS_TSV; // 之前的tsv文件存成了txt
            case "csv" -> CSV;
            case "xls", "xlsx" -> EXCEL;
            case "cfg" -> CFG;
            case "json" -> JSON;
            default -> null;
        };
    }

    public record TableNameIndex(String tableName,
                                 int index) {
    }

    public static @Nullable TableNameIndex getTableNameIndex(Path filePath, String sheetName) {
        Path path;
        if (filePath.getParent() != null) {
            path = filePath.getParent().resolve(sheetName);
        } else {
            path = Path.of(sheetName);
        }
        return getTableNameIndex(path);
    }

    //现在都小写了，要是讲究的话，应该是路径小写，sheetName不改
    public static @Nullable TableNameIndex getTableNameIndex(Path filePath) {
        List<String> codeNames = new ArrayList<>();
        for (Path path : filePath) {
            String fileName = path.getFileName().toString();
            String codeName = FileNameUtil.getCodeName(fileName);
            if (codeName == null) {
                return null;
            }
            codeNames.add(codeName);
        }
        String fullName = String.join(".", codeNames);

        String tableName;
        int index;
        int i = fullName.lastIndexOf("_");
        if (i < 0) {
            tableName = fullName.trim();
            index = 0;
        } else {
            String postfix = fullName.substring(i + 1).trim();
            try {
                index = Integer.parseInt(postfix);
                tableName = fullName.substring(0, i).trim();
            } catch (NumberFormatException ignore) {
                tableName = fullName.trim();
                index = 0;
            }
        }
        return new TableNameIndex(tableName, index);
    }



    public static String getJsonTableDirName(String tableName) {
        return "_" + tableName.replace(".", "_");
    }

    public static Path jsonRecordFileName(String id, FileSystem fileSystem) throws IOException {
        if (id.contains("/") || id.contains("\\") || id.contains("..")) {
            throw new IOException("invalid record id (path separator or '..' not allowed): " + id);
        }
        try {
            Path fileName = fileSystem.getPath(id + ".json");
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
    private static @Nullable String subTableNameIfJsonDir(String dirName) {
        if (!dirName.startsWith("_")) {
            return null;
        }
        String sub = dirName.substring(1);
        // _后要是英文字母
        if (sub.isEmpty() || FileNameUtil.isFirstNotAzChar(sub)) {
            return null;
        }

        // 不能含中文
        int hanIdx = FileNameUtil.findFirstHanIndex(sub);
        if (hanIdx != -1) {
            return null;
        }
        return sub;
    }

    public static @Nullable String getTableNameIfTableDirForJson(String dirName) {
        String sub = subTableNameIfJsonDir(dirName);
        if (sub == null) {
            return null;
        }
        return sub.replace("_", ".");
    }

    /// 从嵌套的 JSON 子目录名提取表名的后半部分。
    /// 如 "_buff" → "buff"，"_instancelogic" → "instancelogic"。
    /// 非 "_" 开头或不符合规则返回 null。
    public static @Nullable String getSubTableNameIfJsonSubDir(String subDirName) {
        return subTableNameIfJsonDir(subDirName);
    }

    public static boolean isTableDirForJson(String dirName) {
        return subTableNameIfJsonDir(dirName) != null;
    }

}
