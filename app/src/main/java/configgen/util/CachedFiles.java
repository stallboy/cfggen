package configgen.util;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 一次生成 run 的输出登记册：writeFile 登记保留文件，deleteOtherFiles/keepMetaAndDeleteOtherFiles
 * 登记待清理目录，finalizeRun 统一删除目录中未登记的文件。
 * 原为全 static 的进程级状态，两个并发的 run（GUI/postRun bat 线程 vs 主线程）会互相清掉对方登记；
 * 现为实例，由 Context 持有（ctx.outputFiles()），随 run 传递。
 */
public class CachedFiles {
    // 表生成并发：writeFile/keepFile 会被多个工作线程同时调用，必须用并发安全 Set
    private final Set<Path> keptPaths = ConcurrentHashMap.newKeySet();

    // finalizeRun 会迭代这两个列表，登记与清理可能来自不同线程，必须用并发安全容器
    private final List<File> deleteFiles = new CopyOnWriteArrayList<>();
    private final List<File> deleteKeepMetaWithSuffixFiles = new CopyOnWriteArrayList<>();
    private static final Set<String> metaSuffixSet = Set.of(".meta", ".uid");

    public void deleteOtherFiles(File dir) {
        deleteFiles.add(dir);
    }

    public void keepMetaAndDeleteOtherFiles(File dir) {
        deleteKeepMetaWithSuffixFiles.add(dir);
    }

    /**
     * 处理登记的清理目录后清空登记：Main.run 每次运行末尾都会调用（不只进程退出），
     * 同一 Context 上 postRun bat 也可能再次触发生成，登记必须按 run 清空。
     * 清空安全：所有要keep的文件每run都会重新登记（writeFile/copyFileIfNotExist都无条件keepFile）
     */
    public void finalizeRun() {
        deleteFiles.stream().filter(File::exists)
                .forEach(f -> doRemoveFile(f, false));
        deleteKeepMetaWithSuffixFiles.forEach(dir ->
                doRemoveFile(dir, true));
        deleteFiles.clear();
        deleteKeepMetaWithSuffixFiles.clear();
        keptPaths.clear();
    }

    public void writeFile(Path path, byte[] data) throws IOException {
        writeChangedFile(path, data, target -> Files.write(target, data, StandardOpenOption.CREATE,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
    }

    public void writeFileReplacement(Path path, byte[] data) throws IOException {
        writeChangedFile(path, data, target -> FileUtil.writeReplacement(target, staged -> Files.write(staged, data)));
    }

    private void writeChangedFile(Path path, byte[] data, FileUtil.StagedFileWriter writer) throws IOException {
        keepFile(path);
        File file = path.toFile();
        if (!file.exists()) {
            Logger.log("create file: " + path);
            mkDirs(java.util.Objects.requireNonNull(path.getParent()).toFile());
        } else {
            // 大小不同时无需读取旧内容。
            if (file.length() == data.length && Arrays.equals(Files.readAllBytes(path), data)) {
                return;
            }
            Logger.log("modify file: " + path);
        }
        writer.write(path);
    }

    public void keepFile(Path path) {
        keptPaths.add(fileKey(path));
    }

    private static void mkDirs(File file) {
        if (!file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.mkdirs();
            // 并发生成时其他线程可能刚建好同一目录使 mkdirs 返回 false，以最终存在为准
            if (!file.exists()) {
                Logger.log("mkdirs fail: " + normalizePath(file.toPath()));
            }
        }
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

    private void doRemoveFile(File file, boolean keepMeta) {
        Path key = fileKey(file.toPath());
        boolean keep = keptPaths.contains(key);
        if (keep) {
            return;
        }

        if (keepMeta) {
            Path noMetaKey = findNoMetaKey(key);
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
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    doRemoveFile(f, keepMeta);
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

    private static @Nullable Path findNoMetaKey(Path key) {
        Path name = key.getFileName();
        if (name == null) {
            return null;
        }
        String filename = name.toString();
        String lowerFilename = filename.toLowerCase(Locale.ROOT);
        for (String metaSuffix : metaSuffixSet) {
            if (lowerFilename.endsWith(metaSuffix) && filename.length() > metaSuffix.length()) {
                return key.resolveSibling(filename.substring(0, filename.length() - metaSuffix.length()));
            }
        }
        return null;
    }


}
