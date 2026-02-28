package configgen.util;

import configgen.gen.Generator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class FileUtil {

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


    public static void copyFileIfNotExist(String sourceFileInResources, String fallbackSourceFile,
                                             Path dstFile, String dstEncoding) throws IOException {

        if (Files.exists(dstFile)) {
            CachedFiles.keepFile(dstFile);
            return;
        }

        // 1. 优先尝试从类路径获取
        InputStream is = Generator.class.getResourceAsStream(sourceFileInResources);

        // 2. 找不到则尝试从物理磁盘查找源码
        if (is == null) {
            File physicalFile = findSourceFile(fallbackSourceFile);
            if (physicalFile.exists()) {
                is = new FileInputStream(physicalFile);
            }
        }

        if (is == null) {
            throw new FileNotFoundException("Could not find source: " + sourceFileInResources);
        }

        // 3. 使用 try-with-resources 确保所有流（包括 InputStream）都被正确关闭
        try (InputStream autoCloseIs = is;
             BufferedReader br = new BufferedReader(new UnicodeReader(autoCloseIs, StandardCharsets.UTF_8));
             CachedIndentPrinter ps = new CachedIndentPrinter(dstFile, dstEncoding)) {

            String line;
            while ((line = br.readLine()) != null) {
                ps.println(line);
            }
        }
    }

    private static File findSourceFile(String fallbackPath) {
        try {
            // 获取 Generator.class 所在的物理位置 (可能是 JAR 包路径，也可能是 target/classes 目录)
            Path codePath = Path.of(Generator.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            // 如果是在 IDE 中运行，通常在 project/target/classes
            // 我们需要向上找，直到找到包含 src 的目录
            Path current = codePath;
            while (current != null) {
                Path candidate = current.resolve(fallbackPath);
                if (Files.exists(candidate)) {
                    return candidate.toFile();
                }
                current = current.getParent();
            }
        } catch (Exception ignored) {
        }
        return new File(fallbackPath); // 最后的倔强：尝试当前工作目录
    }

}
