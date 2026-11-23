package configgen.layout;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * 数据文件命名约定的解析：从带中文后缀/后缀名的文件名提取code名。
 */
public final class FileNameUtil {
    private FileNameUtil() {
    }

    public static boolean isFileIgnored(Path path) {
        return path.toFile().isHidden() || path.getFileName().toString().startsWith("~");
    }

    public static @Nullable String getCodeName(String fileName) {
        if (!startsWithAsciiLetter(fileName)) {
            return null;
        }

        int dot = fileName.indexOf('.');
        if (dot >= 0) {
            fileName = fileName.substring(0, dot);
        }

        int han = findFirstHanIndex(fileName);
        if (han >= 0) {
            int end = fileName.charAt(han - 1) == '_' ? han - 1 : han;
            fileName = fileName.substring(0, end);
        }
        return fileName.toLowerCase(Locale.ROOT);
    }

    static boolean startsWithAsciiLetter(String name) {
        if (name.isEmpty()) {
            return false;
        }
        char first = name.charAt(0);
        return ('a' <= first && first <= 'z') || ('A' <= first && first <= 'Z');
    }

    static int findFirstHanIndex(String s) {
        for (int i = 0; i < s.length(); ) {
            int codepoint = s.codePointAt(i);
            if (Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN) {
                return i;
            }
            i += Character.charCount(codepoint);
        }
        return -1;
    }

    static @Nullable Path findModuleDirectory(Path parent, String moduleName) throws IOException {
        try (Stream<Path> paths = Files.list(parent)) {
            for (Path path : paths.toList()) {
                if (Files.isDirectory(path) && moduleName.equals(getCodeName(path.getFileName().toString()))) {
                    return path;
                }
            }
        }
        return null;
    }
}
