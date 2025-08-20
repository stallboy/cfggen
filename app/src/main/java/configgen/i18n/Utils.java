package configgen.i18n;

import java.io.File;
import java.util.Objects;
import java.util.regex.Pattern;

public class Utils {
    private static final Pattern pattern = Pattern.compile("\r\n");

    public static String normalize(String text) {
        return pattern.matcher(text).replaceAll("\n");
    }

    public static void moveDirFilesToAnotherDir(String from, String to) {
        File toDir = new File(to);
        if (toDir.isDirectory()) {
            for (File file : Objects.requireNonNull(toDir.listFiles())) {
                if (!file.delete()) {
                    throw new RuntimeException("delete " + file + " failed");
                }
            }
        } else {
            if (!toDir.mkdirs()) {
                throw new RuntimeException("mkdir " + toDir + " failed");
            }
        }
        for (File file : Objects.requireNonNull(new File(from).listFiles())) {
            if (!file.renameTo(new File(to, file.getName()))) {
                throw new RuntimeException("rename %s to %s failed".formatted(from, to));
            }
        }
    }
}
