package configgen.ctx;

import com.sun.nio.file.ExtendedWatchEventModifier;
import configgen.data.DataUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.nio.file.StandardWatchEventKinds.*;

public class Watcher {
    private final Path rootDir;
    private volatile Map<String, Long> fileLastModifiedMap;

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

    private void initFileLastModifiedMap() {

    }

    public void watchLoop() throws IOException, InterruptedException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        rootDir.register(watchService, new WatchEvent.Kind<?>[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY},
                ExtendedWatchEventModifier.FILE_TREE);


        WatchKey key;
        while ((key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                @SuppressWarnings("unchecked")
                WatchEvent<Path> evt = (WatchEvent<Path>) event;
                Path path = evt.context();
                File file = path.toFile();
                if (!file.isFile()) {
                    continue;
                }
                if (file.isHidden() || path.getFileName().startsWith("~")) {
                    continue;
                }
                if (evt.kind() == ENTRY_CREATE) {
                    System.out.println("+ " + path + " " + file.lastModified());
                } else if (evt.kind() == ENTRY_DELETE) {
                    System.out.println("- " + path);
                } else {
                    System.out.println("x " + path + " " + file.lastModified());
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

