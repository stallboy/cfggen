package configgen.ctx;

import configgen.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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
    private WatchService watchService;
    private volatile boolean recursiveSupport;
    // ENTRY_CREATE新目录时由轮询线程写入；stop后若旧线程迟迟未退出、实例被重启，存在跨线程访问，用并发map防损坏
    private final Map<WatchKey, Path> keys = new ConcurrentHashMap<>();

    public Watcher(Path rootDir, ExplicitDir explicitDir) {
        Objects.requireNonNull(rootDir);
        this.rootDir = rootDir;
        this.explicitDir = explicitDir;
    }

    /**
     * 创建WatchService和初始目录注册在调用线程内同步完成，start()返回后事件即可被捕获：
     * 注册在轮询线程里异步做的话，调用方（如测试、server启动）在注册完成前操作的文件会漏检。
     * 注册失败直接抛出（如rootDir不存在），不再退化为"静默无监听"。
     */
    public synchronized void start() {
        if (startedThread != null) {
            throw new IllegalStateException("already started");
        }
        WatchService ws;
        try {
            ws = FileSystems.getDefault().newWatchService();
            keys.clear();
            recursiveSupport = registerRoot(ws);
        } catch (IOException e) {
            throw new UncheckedIOException("Watcher register failed for " + rootDir, e);
        }
        watchService = ws;
        startedThread = Thread.startVirtualThread(() -> {
            try {
                watchLoop(ws);
            } catch (IOException | InterruptedException | ClosedWatchServiceException e) {
                Logger.log("Watcher stopped by %s", e.toString());
            }
        });
    }

    // watchService.take()不响应interrupt（JDK已知限制），必须close让take()抛ClosedWatchServiceException才能退出。
    // start()同步完成注册后watchService必非null，这里总能close到，不会出现线程永久挂在take()上的泄漏
    public synchronized void stop() {
        Thread thread = startedThread;
        if (thread == null) {
            return;
        }
        startedThread = null;

        WatchService ws = watchService;
        if (ws != null) {
            try {
                ws.close();
            } catch (IOException e) {
                Logger.verbose("close watch service err: %s", e.toString());
            }
        }

        thread.interrupt();
        try {
            thread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (thread.isAlive()) {
            Logger.log("Watcher thread did not stop in 5s");
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

    private boolean registerRoot(WatchService watcher) throws IOException {
        // 跨平台兼容的目录注册方式
        try {
            // 尝试使用FILE_TREE（仅Windows支持）
            WatchEvent.Modifier modifier = (WatchEvent.Modifier) Class
                    .forName("com.sun.nio.file.ExtendedWatchEventModifier")
                    .getField("FILE_TREE")
                    .get(null);
            rootDir.register(watcher, new WatchEvent.Kind<?>[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY},
                    modifier);
            return true;
        } catch (Exception e) {
            // 回退到手动递归监控
            registerAll(rootDir, watcher);
            return false;
        }
    }

    private void register(Path dir, WatchService watcher) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
    }

    private void registerAll(final Path start, WatchService watcher) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir,
                                                              @NotNull BasicFileAttributes attrs)
                    throws IOException {
                register(dir, watcher);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void watchLoop(WatchService watchService) throws IOException, InterruptedException {
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
                    // 事件队列溢出说明有事件丢失（如git checkout一次性改几十个文件），必须触发一次全量reload自愈，
                    // 静默丢弃会导致溢出期间的变更若再无后续事件就永远丢失
                    trigger();
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
