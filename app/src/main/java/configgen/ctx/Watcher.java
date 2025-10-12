package configgen.ctx;

import com.sun.nio.file.ExtendedWatchEventModifier;
import configgen.util.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.Objects;

import static configgen.data.DataUtil.*;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * 监控目录rootDir下文件，有变化则设置标记lastEvtSec
 */
public class Watcher {
    private final Path rootDir;
    private long lastEvtSec;
    private final Object lock = new Object();

    public Watcher(Path rootDir) {
        Objects.requireNonNull(rootDir);
        this.rootDir = rootDir;
    }

    public Thread start() {
        return Thread.startVirtualThread(() -> {
            try {
                watchLoop();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public long getLastEvtSecAndReset() {
        synchronized (lock) {
            long sec = lastEvtSec;
            lastEvtSec = 0;
            return sec;
        }
    }

    private void trigger(){
        synchronized (lock){
            lastEvtSec = Instant.now().getEpochSecond();
        }
    }

    private void watchLoop() throws IOException, InterruptedException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        rootDir.register(watchService, new WatchEvent.Kind<?>[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY},
                ExtendedWatchEventModifier.FILE_TREE);


        WatchKey key;
        while ((key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                Path path = (Path) event.context();
                Logger.verbose(event.kind() + "  " + path);
                File file = path.toFile();
                if (file.isDirectory()) {
                    trigger();
                } else if (file.isFile()) {
                    if (isFileIgnored(path)) {
                        continue;
                    }
                    Path fileName = path.getFileName();
                    FileFmt fmt = getFileFormat(fileName);
                    if (fmt == null) {
                        continue;
                    }
                    if (fmt == FileFmt.CSV) {
                        if (isFirstNotAzChar(fileName.toString())) {
                            continue;
                        }
                    } else if (fmt == FileFmt.JSON) {
                        String dirName = path.getParent().getFileName().toString();
                        if (!isTableDirForJson(dirName)) {
                            continue;
                        }
                    }
                    trigger();
                }
            }

            key.reset();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Thread t = new Watcher(Path.of(".")).start();
        t.join();
    }
}

