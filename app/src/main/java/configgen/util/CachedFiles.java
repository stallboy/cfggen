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
    private static final Comparator<String> SUFFIX_ORDER =
            Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder());

    private final Set<Path> keptPaths = ConcurrentHashMap.newKeySet();
    private final Set<Path> createdPaths = ConcurrentHashMap.newKeySet();
    private final Map<Path, Set<String>> directories = new LinkedHashMap<>();

    /** Prepare a directory before a generator writes to it. */
    public synchronized void prepareOutputDirectory(Path directory, String... metadataSuffixes) throws IOException {
        Path root = fileKey(directory);
        for (Path registered : directories.keySet()) {
            if (!root.equals(registered) && (root.startsWith(registered) || registered.startsWith(root))) {
                throw new IOException("Overlapping output directories: " + registered + " and " + root);
            }
        }

        prepareMarker(root);
        Set<String> suffixes = directories.computeIfAbsent(root, ignored -> new TreeSet<>(SUFFIX_ORDER));
        for (String suffix : metadataSuffixes) {
            suffixes.add(suffix.toLowerCase(Locale.ROOT));
        }
    }

    private void prepareMarker(Path root) throws IOException {
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
                if (entries.anyMatch(entry -> !createdPaths.contains(entry)
                        || !Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS))) {
                    throw new IOException("Output directory is not empty and has no regular output marker: " + root
                            + " (expected " + OUTPUT_MARKER + ")");
                }
            }
            Files.createFile(marker);
        }
        keepFile(marker);
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
        createdPaths.clear();
    }

    public void writeFile(Path path, byte[] data) throws IOException {
        writeFile(path, data, false);
    }

    public void writeFileReplacement(Path path, byte[] data) throws IOException {
        writeFile(path, data, true);
    }

    private void writeFile(Path path, byte[] data, boolean replace) throws IOException {
        Path key = fileKey(path);
        keepFile(key);
        boolean newFile = Files.notExists(key, LinkOption.NOFOLLOW_LINKS);
        if (replace) {
            FileUtil.replaceIfChanged(path, data);
        } else {
            FileUtil.writeIfChanged(path, data);
        }
        if (newFile) {
            createdPaths.add(key);
        }
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

    private void doRemoveFile(File file, Path root, Set<String> metadataSuffixes) {
        Path key = fileKey(file.toPath());
        if (keptPaths.contains(key)) {
            return;
        }

        if (!metadataSuffixes.isEmpty() && !key.equals(root)) {
            Path asset = findNoMetaKey(key, metadataSuffixes);
            if (asset != null && (keptPaths.contains(asset) || Files.isDirectory(asset, LinkOption.NOFOLLOW_LINKS))) {
                return;
            }
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
                if (!metadataSuffixes.isEmpty()) {
                    // Clean each asset before deciding whether its longer metadata filename should survive.
                    Arrays.sort(files, Comparator.comparingInt(entry -> entry.getName().length()));
                }
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

    private static @Nullable Path findNoMetaKey(Path key, Set<String> metadataSuffixes) {
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
