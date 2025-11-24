package configgen.ctx;

import configgen.util.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static configgen.data.DataUtil.*;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * 监控目录rootDir下文件，有变化则设置标记lastEvtMillis
 */
public class Watcher {
    private final Path rootDir;
    private final ExplicitDir explicitDir;
    private volatile long lastEvtMillis;
    private final AtomicInteger eventVersion = new AtomicInteger(0);
    private Thread startedThread;
    private final Map<WatchKey, Path> keys = new HashMap<>();

    public Watcher(Path rootDir, ExplicitDir explicitDir) {
        Objects.requireNonNull(rootDir);
        this.rootDir = rootDir;
        this.explicitDir = explicitDir;
    }

    public void start() {
        startedThread = Thread.startVirtualThread(() -> {
            try {
                watchLoop();
            } catch (IOException | InterruptedException e) {
                Logger.log("Watcher stopped by %s", e.toString());
            }
        });
    }

    public void stop() {
        if (startedThread == null) {
            return;
        }
        startedThread.interrupt();
        try {
            startedThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            startedThread = null;
        }
    }

    public long getLastEventMillis() {
        return lastEvtMillis;
    }

    public int getEventVersion() {
        return eventVersion.get();
    }

    private void trigger() {
        lastEvtMillis = System.currentTimeMillis();
        eventVersion.incrementAndGet();
    }

    private void register(Path dir, WatchService watcher) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
    }

    private void registerAll(final Path start, WatchService watcher) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                register(dir, watcher);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void watchLoop() throws IOException, InterruptedException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        keys.clear();
        boolean recursiveSupport = false;

        // 跨平台兼容的目录注册方式
        try {
            // 尝试使用FILE_TREE（仅Windows支持）
            WatchEvent.Modifier modifier = (WatchEvent.Modifier) Class
                    .forName("com.sun.nio.file.ExtendedWatchEventModifier")
                    .getField("FILE_TREE")
                    .get(null);
            rootDir.register(watchService, new WatchEvent.Kind<?>[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY},
                    modifier);
            recursiveSupport = true;
        } catch (Exception e) {
            // 回退到手动递归监控
            registerAll(rootDir, watchService);
        }

        WatchKey key;
        while ((key = watchService.take()) != null) {
            Path dir = keys.get(key);
            if (dir == null) {
                // 如果是FILE_TREE模式，key没有存入map，dir默认为rootDir
                // 如果是标准模式，理论上不应该为null，除非有未预期的key
                dir = rootDir;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path contextPath = ev.context();

                Path fullPath;
                Path relativePath;

                if (recursiveSupport) {
                    // FILE_TREE模式下，contextPath是相对于rootDir的路径
                    relativePath = contextPath;
                    fullPath = rootDir.resolve(relativePath);
                } else {
                    // 标准模式下，contextPath是文件名
                    fullPath = dir.resolve(contextPath);
                    relativePath = rootDir.relativize(fullPath);
                }

                Logger.verbose(kind + "  " + relativePath);

                // 如果是标准模式，且是新建目录，需要注册
                if (!recursiveSupport && kind == ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(fullPath, LinkOption.NOFOLLOW_LINKS)) {
                            registerAll(fullPath, watchService);
                        }
                    } catch (IOException ignored) {
                    }
                }

                if (kind == ENTRY_DELETE) {
                    // 对于删除事件，文件已不存在，只能通过路径判断
                    handleFileEvent(relativePath);
                } else {
                    // 对于创建和修改事件，可以检查文件属性
                    if (Files.isDirectory(fullPath)) {
                        trigger();
                    } else if (Files.isRegularFile(fullPath)) {
                        handleFileEvent(relativePath);
                    }
                }
            }

            boolean valid = key.reset();
            if (!valid && !recursiveSupport) {
                keys.remove(key);
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    private void handleFileEvent(Path relativePath) {
        if (isFileIgnored(relativePath)) {
            Logger.verbose("File ignored: " + relativePath);
            return;
        }

        Path fileName = relativePath.getFileName();
        FileFmt fmt = getFileFormat(fileName);
        if (fmt == null) {
            return;
        }

        switch (fmt) {
            case CSV, EXCEL, CFG -> {
                if (explicitDir != null) {
                    Path topDir = relativePath.getName(0);
                    String dirName = topDir.getFileName().toString();
                    if (!explicitDir.excelFileDirs().contains(dirName)) {
                        return;
                    }
                }
            }
            case JSON -> {
                Path parent = relativePath.getParent();
                if (parent == null)
                    return;
                String dirName = parent.getFileName().toString();
                if (!isTableDirForJson(dirName)) {
                    return;
                }
                if (explicitDir != null && !explicitDir.jsonFileDirs().contains(dirName)) {
                    return;
                }
            }
            case TXT_AS_TSV -> {
                if (explicitDir == null) {
                    return;
                }
                Path parent = relativePath.getParent();
                if (parent == null)
                    return;
                String dirName = parent.getFileName().toString();
                if (!explicitDir.txtAsTsvFileInThisDirAsInRoot_To_AddTag_Map().containsKey(dirName)) {
                    return;
                }
            }
        }

        Logger.verbose("Triggering watcher for file: " + relativePath);
        trigger();
    }

}
