package configgen.ctx;

import configgen.util.Logger;

import java.io.IOException;
import java.nio.file.*;
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
        Logger.verbose("Watcher triggered, version: " + eventVersion.get());
    }

    private void watchLoop() throws IOException, InterruptedException {
        WatchService watchService = FileSystems.getDefault().newWatchService();

        // 跨平台兼容的目录注册方式
        try {
            // 尝试使用FILE_TREE（仅Windows支持）
            try {
                @SuppressWarnings("unchecked")
                WatchEvent.Modifier modifier = (WatchEvent.Modifier) Class
                    .forName("com.sun.nio.file.ExtendedWatchEventModifier")
                    .getField("FILE_TREE")
                    .get(null);
                rootDir.register(watchService, new WatchEvent.Kind<?>[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY}, modifier);
                Logger.verbose("Using FILE_TREE modifier for recursive directory watching");
            } catch (Exception e) {
                // 回退到非递归监控
                rootDir.register(watchService, new WatchEvent.Kind<?>[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY});
                Logger.verbose("Using standard directory watching (non-recursive)");
            }

            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path relativePath = (Path) event.context();
                    Logger.verbose(event.kind() + "  " + relativePath);

                    // 构建完整路径
                    Path fullPath = rootDir.resolve(relativePath);

                    // 通过事件类型和文件扩展名判断，避免不必要的文件系统调用
                    WatchEvent.Kind<?> kind = event.kind();

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

                key.reset();
            }
        } catch (ClosedWatchServiceException e) {
            // 正常关闭，忽略异常
            Logger.verbose("WatchService closed normally");
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
            Logger.verbose("File format not supported: " + relativePath);
            return;
        }

        // 原有的格式判断逻辑...
        switch (fmt) {
            case CSV, EXCEL -> {
                Logger.verbose("Processing CSV/Excel file: " + relativePath);
                if (explicitDir != null) {
                    Path topDir = relativePath.getName(0);
                    String dirName = topDir.getFileName().toString();
                    if (!explicitDir.excelFileDirs().contains(dirName)) {
                        Logger.verbose("CSV/Excel file directory not in explicitDir: " + dirName);
                        return;
                    }
                } else {
                    Logger.verbose("explicitDir is null, accepting CSV/Excel file");
                }
            }
            case JSON -> {
                Path parent = relativePath.getParent();
                if (parent == null) return;
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
                if (parent == null) return;
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

