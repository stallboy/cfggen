package configgen.util;

import org.jspecify.annotations.Nullable;

import configgen.gen.Generator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
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
        if (!Files.isRegularFile(from)) {
            return;
        }
        try {
            moveReplacing(from, to);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to replace " + to + " with " + from, e);
        }
    }

    @FunctionalInterface
    public interface StagedFileWriter {
        void write(Path stagingFile) throws IOException;
    }

    public static void writeReplacement(Path path, StagedFileWriter writer) throws IOException {
        Path target = path.toAbsolutePath();
        if (Files.isSymbolicLink(target)) {
            target = target.toRealPath();
        }
        Path staged = Files.createTempFile(target.getParent(), "cfggen-", ".tmp");
        try {
            writer.write(staged);
            PosixFileAttributeView attributes = Files.getFileAttributeView(target, PosixFileAttributeView.class);
            //noinspection ConstantValue
            if (attributes != null) {
                try {
                    Files.setPosixFilePermissions(staged, attributes.readAttributes().permissions());
                } catch (NoSuchFileException ignored) {
                    // 没有旧文件可继承权限。
                }
            }
            moveReplacing(staged, target);
        } catch (IOException | RuntimeException | Error failure) {
            try {
                Files.deleteIfExists(staged);
            } catch (IOException | RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        if (Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileSystemException(target.toString(), null, "Cannot replace a directory with a file");
        }
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
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

    public static void assureFileExistIf(@Nullable String filePath) {
        if (filePath != null) {
            Path root = Path.of(".");
            if (!Files.exists(root.resolve(filePath))) {
                throw new IllegalArgumentException(filePath + " not exist");
            }
        }
    }


    public static void copyFileIfNotExist(String sourceFileInResources, String fallbackSourceFile,
                                             Path dstFile, String dstEncoding, CachedFiles cachedFiles) throws IOException {
        copyFileIfNotExist(sourceFileInResources, fallbackSourceFile, dstFile, dstEncoding, cachedFiles, line -> line);
    }

    public static void copyFileIfNotExist(String sourceFileInResources, String fallbackSourceFile,
                                         Path dstFile, String dstEncoding, CachedFiles cachedFiles,
                                         java.util.function.UnaryOperator<String> transformLine) throws IOException {

        if (Files.exists(dstFile)) {
            cachedFiles.keepFile(dstFile);
            return;
        }

        try (InputStream source = openSupportSource(sourceFileInResources, fallbackSourceFile);
             BufferedReader br = new BufferedReader(new UnicodeReader(source, StandardCharsets.UTF_8));
             CachedIndentPrinter ps = new CachedIndentPrinter(dstFile, dstEncoding, cachedFiles)) {
            String line;
            while ((line = br.readLine()) != null) {
                ps.println(transformLine.apply(line));
            }
        }
    }

    public static InputStream openSupportSource(String sourceFileInResources, String fallbackSourceFile)
            throws IOException {
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

        return is;
    }

    private static File findSourceFile(String fallbackPath) {
        try {
            // 获取 Generator.class 所在的物理位置 (可能是 JAR 包路径，也可能是 target/classes 目录)
            // 如果是在 IDE 中运行，通常在 project/target/classes
            // 我们需要向上找，直到找到包含 src 的目录
            Path current = Path.of(Generator.class.getProtectionDomain().getCodeSource().getLocation().toURI());
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
