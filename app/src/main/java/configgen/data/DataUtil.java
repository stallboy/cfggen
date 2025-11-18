package configgen.data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    public static FileFmt getFileFormat(Path path) {
        String fileName = path.getFileName().toString();
        String ext = "";
        int i = fileName.lastIndexOf('.');
        if (i >= 0) {
            ext = fileName.substring(i + 1).toLowerCase();
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

    public static TableNameIndex getTableNameIndex(Path filePath, String sheetName) {
        Path path;
        if (filePath.getParent() != null) {
            path = filePath.getParent().resolve(sheetName);
        } else {
            path = Path.of(sheetName);
        }
        return getTableNameIndex(path);
    }

    //现在都小写了，要是讲究的话，应该是路径小写，sheetName不改
    public static TableNameIndex getTableNameIndex(Path filePath) {
        List<String> codeNames = new ArrayList<>();
        for (Path path : filePath) {
            String fileName = path.getFileName().toString();
            String codeName = getCodeName(fileName);
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

    public static String getCodeName(String fileName) {
        if (fileName.isEmpty()) {
            return null;
        }

        // 只接受首字母是英文字母的
        if (isFirstNotAzChar(fileName)) {
            return null;
        }

        // 不要后缀
        int i = fileName.indexOf('.');
        if (i >= 0) {
            fileName = fileName.substring(0, i);
        }

        // 有没有汉字
        int hanIdx = findFirstHanIndex(fileName);
        if (hanIdx == -1) {
            return fileName.toLowerCase(); // 所有的文件名都小写，但最后尊重cfg文件里的大小写
        }

        // 只要汉字前的，不包括_
        int end = hanIdx;
        if (fileName.charAt(hanIdx - 1) == '_') {
            end = hanIdx - 1;
        }
        return fileName.substring(0, end).toLowerCase();
    }

    public static boolean isFirstNotAzChar(String name) {
        char firstChar = name.charAt(0);
        return ('a' > firstChar || firstChar > 'z') && ('A' > firstChar || firstChar > 'Z');
    }

    private static int findFirstHanIndex(String s) {
        for (int i = 0; i < s.length(); ) {
            int codepoint = s.codePointAt(i);
            if (Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN) {
                return i;
            }
            i += Character.charCount(codepoint);
        }
        return -1;
    }


    public static Path getJsonTableDir(Path dataDir, String tableName) {
        String dirName = "_" + tableName.replace(".", "_");
        return dataDir.resolve(dirName);
    }

    public static String getTableNameIfTableDirForJson(String dirName) {
        if (!dirName.startsWith("_")) {
            return null;
        }
        String sub = dirName.substring(1);
        // _后要是英文字母
        if (isFirstNotAzChar(sub)) {
            return null;
        }

        // 不能含中文
        int hanIdx = findFirstHanIndex(sub);
        if (hanIdx != -1) {
            return null;
        }

        return sub.replace("_", ".");
    }

    public static boolean isTableDirForJson(String dirName) {
        if (!dirName.startsWith("_")) {
            return false;
        }
        String sub = dirName.substring(1);
        // _后要是英文字母
        if (isFirstNotAzChar(sub)) {
            return false;
        }

        // 不能含中文
        int hanIdx = findFirstHanIndex(sub);
        return hanIdx == -1;
    }

    // 在DataUtil类中添加写入器选择方法
    public static ExcelReader getExcelWriter(boolean useFastExcel) {
        if (useFastExcel) {
            return WriteByFastExcel.INSTANCE;
        } else {
            ExcelReader poiWriter = configgen.gen.BuildSettings.getPoiWriter();
            return poiWriter != null ? poiWriter : WriteByFastExcel.INSTANCE;
        }
    }

}
