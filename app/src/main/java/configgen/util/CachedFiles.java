package configgen.util;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** One generation batch's retained files and managed output directories. */
public class CachedFiles {
    private static final String OUTPUT_MARKER = ".cfggen-output";

    private final Set<Path> keptPaths = ConcurrentHashMap.newKeySet();
    // Directory preparation and finalization run on the batch thread.
    private final Map<Path, List<String>> directories = new LinkedHashMap<>();

    /**
     * Mark a new or empty directory, or verify the marker of an existing one.
     * Call before writing any output under the directory.
     */
    public void prepareOutputDirectory(Path directory, String... metadataSuffixes) throws IOException {
        Path root = fileKey(directory);
        List<String> suffixes = Arrays.stream(metadataSuffixes)
                .map(suffix -> suffix.toLowerCase(Locale.ROOT))
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()))
                .toList();

        for (var entry : directories.entrySet()) {
            Path registered = entry.getKey();
            if (root.equals(registered)) {
                if (!suffixes.equals(entry.getValue())) {
                    throw new IOException("Conflicting output directory registration: " + root);
                }
            } else if (root.startsWith(registered) || registered.startsWith(root)) {
                throw new IOException("Overlapping output directories: " + registered + " and " + root);
            }
        }

        Path existing = root;
        while (Files.notExists(existing, LinkOption.NOFOLLOW_LINKS) && existing.getParent() != null) {
            existing = existing.getParent();
        }
        if (!isDirectDirectory(existing)) {
            throw new IOException("Output path is not a directory or redirects to another path: " + existing);
        }
        Files.createDirectories(root);
        if (!isDirectDirectory(root)) {
            throw new IOException("Output directory redirects to another path: " + root);
        }
        Path marker = root.resolve(OUTPUT_MARKER);
        if (!Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS)) {
            try (var entries = Files.list(root)) {
                if (entries.findAny().isPresent()) {
                    throw new IOException("Output directory is not empty and has no regular output marker: " + root
                            + " (expected " + OUTPUT_MARKER + ")");
                }
            }
            Files.createFile(marker);
        }
        keepFile(marker);
        directories.put(root, suffixes);
    }

    private static boolean isDirectDirectory(Path path) throws IOException {
        return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) && path.toRealPath().equals(path);
    }

    /** Remove stale output only after all generators in the batch have succeeded. */
    public void finalizeRun() {
        for (var entry : directories.entrySet()) {
            Path root = entry.getKey();
            try {
                Path marker = root.resolve(OUTPUT_MARKER);
                if (!isDirectDirectory(root) || !Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS)) {
                    Logger.log("skip cleanup: output directory or marker changed: " + root);
                    continue;
                }
                keepFile(marker);
                doRemoveFile(root.toFile(), root, entry.getValue());
            } catch (IOException e) {
                Logger.log("skip cleanup: " + root + ": " + e.getMessage());
            }
        }
        directories.clear();
        keptPaths.clear();
    }

    public void writeFile(Path path, byte[] data) throws IOException {
        keepFile(path);
        FileUtil.writeIfChanged(path, data);
    }

    public void writeFileReplacement(Path path, byte[] data) throws IOException {
        keepFile(path);
        FileUtil.replaceIfChanged(path, data);
    }

    public void keepFile(Path path) {
        keptPaths.add(fileKey(path));
    }

    private static Path fileKey(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private static String normalizePath(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    private static boolean delete(File file) {
        String dir = file.isDirectory() ? "dir" : "file";
        boolean deleteOk = file.delete();
        String status = deleteOk ? "" : " fail";
        Logger.log("delete " + dir + status + ": " + normalizePath(file.toPath()));
        return deleteOk;
    }

    private void doRemoveFile(File file, Path root, List<String> metadataSuffixes) {
        Path key = fileKey(file.toPath());
        boolean keep = keptPaths.contains(key);
        if (keep) {
            return;
        }

        if (!metadataSuffixes.isEmpty()) {
            Path noMetaKey = findNoMetaKey(key, metadataSuffixes);
            if (noMetaKey != null) {
                keep = keptPaths.contains(noMetaKey);
                if (!keep && Files.isDirectory(noMetaKey, LinkOption.NOFOLLOW_LINKS)) {
                    for (Path f : keptPaths) {
                        if (f.startsWith(noMetaKey)) {
                            keep = true;
                            break;
                        }
                    }
                }
            }
        }

        if (keep) {
            return;
        }

        if (Files.isDirectory(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            // Windows 目录联接即使指定 NOFOLLOW_LINKS 也可能报告为目录。
            try {
                Path parent = key.getParent();
                if (parent != null && !file.toPath().toRealPath().equals(parent.toRealPath().resolve(key.getFileName()))) {
                    return;
                }
            } catch (IOException e) {
                Logger.log("skip cleanup: " + key + ": " + e.getMessage());
                return;
            }
            if (!key.equals(root) && Files.exists(key.resolve(OUTPUT_MARKER), LinkOption.NOFOLLOW_LINKS)) {
                return;
            }
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    doRemoveFile(f, root, metadataSuffixes);
                }
            }
            File[] newFiles = file.listFiles();
            if (newFiles != null && newFiles.length == 0) {
                delete(file);
            }
        } else {
            delete(file);
        }
    }

    private static @Nullable Path findNoMetaKey(Path key, List<String> metadataSuffixes) {
        Path name = key.getFileName();
        if (name == null) {
            return null;
        }
        String filename = name.toString();
        String lowerFilename = filename.toLowerCase(Locale.ROOT);
        for (String metaSuffix : metadataSuffixes) {
            if (lowerFilename.endsWith(metaSuffix) && filename.length() > metaSuffix.length()) {
                return key.resolveSibling(filename.substring(0, filename.length() - metaSuffix.length()));
            }
        }
        return null;
    }


}
