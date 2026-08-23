package configgen.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    private final Set<String> filename_set = ConcurrentHashMap.newKeySet();

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
        filename_set.clear();
    }

    public void writeFile(Path path, byte[] data) throws IOException {
        keepFile(path);
        if (!path.toFile().exists()) {
            Logger.log("create file: " + path);
            mkDirs(path.getParent().toFile());
            Files.write(path, data, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            return;
        }

        // 大小不同则内容必然变化，直接写入，避免读取整个旧文件做逐字节比较
        if (path.toFile().length() != data.length) {
            Logger.log("modify file: " + path);
            Files.write(path, data, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            return;
        }

        byte[] buf = Files.readAllBytes(path);
        if (!Arrays.equals(buf, data)) {
            Logger.log("modify file: " + path);
            Files.write(path, data, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    public void keepFile(Path path) {
        filename_set.add(fileKey(path));
    }

    private static void mkDirs(File file) {
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Logger.log("mkdirs fail: " + normalizePath(file.toPath()));
            }
        }
    }

    private static String fileKey(Path path) {
        return path.toAbsolutePath().normalize().toString().toLowerCase();
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
        String key = fileKey(file.toPath());
        boolean keep = filename_set.contains(key);
        if (keep) {
            return;
        }

        if (keepMeta) {
            String noMetaKey = findNoMetaKey(key);
            if (noMetaKey != null) {
                keep = filename_set.contains(noMetaKey);
                if (!keep && new File(noMetaKey).isDirectory()) {
                    for (String f : filename_set) {
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

        if (file.isDirectory()) {
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

    private static String findNoMetaKey(String key) {
        for (String metaSuffix : metaSuffixSet) {
            if (key.endsWith(metaSuffix)) {
                return key.substring(0, key.length() - metaSuffix.length());
            }
        }
        return null;
    }


}
