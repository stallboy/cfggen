package configgen.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class FileUtils {

    public static void moveDirFilesToAnotherDir(Path from, Path to) {
        File toDir = to.toFile();
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
        for (File file : Objects.requireNonNull(from.toFile().listFiles())) {
            if (!file.renameTo(to.resolve(file.getName()).toFile())) {
                throw new RuntimeException("rename %s to %s failed".formatted(from, to));
            }
        }
    }

    public static void moveOneFile(Path from, Path to) {
        File toFile = to.toFile();
        if (toFile.exists() && toFile.isFile()) {
            if (!toFile.delete()) {
                throw new RuntimeException("delete " + toFile + " failed");
            }
        }

        File fromFile = from.toFile();
        if (fromFile.exists() && fromFile.isFile()) {
            if (!fromFile.renameTo(toFile)) {
                throw new RuntimeException("rename %s to %s failed".formatted(from, to));
            }
        }
    }

    public static boolean hasFiles(Path dir) {
        File file = dir.toFile();
        if (!file.exists()) {
            return false;
        }
        if (!file.isDirectory()) {
            return false;
        }
        String[] files = file.list();
        return files != null && files.length > 0;
    }

    public static void assureFileExistIf(String filePath) {
        if (filePath != null) {
            Path root = Path.of(".");
            if (!Files.exists(root.resolve(filePath))) {
                throw new IllegalArgumentException(filePath + " not exist");
            }
        }
    }
}
