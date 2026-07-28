
import config.ConfigCodeSchema;
import config.ConfigMgr;
import config.ConfigMgrLoader;
import config.task.Task;
import configgen.genjava.BytesInspector;
import configgen.genjava.CodeDataInspector;
import configgen.genjava.CodeDataPrinter;
import configgen.genjava.ConfigInput;
import configgen.genjava.Schema;
import configgen.genjava.SchemaCompatibleException;

import java.io.IOException;
import java.nio.file.*;
//import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LoadConfig {

    private static final Logger logger = Logger.getLogger("LoadConfig");

    public static void load(String fn) throws IOException {
        Schema codeSchema = ConfigCodeSchema.getCodeSchema();
        try (ConfigInput input = new ConfigInput(Path.of(fn))) {
            Schema dataSchema = ConfigMgrLoader.loadSchema(input);
            boolean compatible = codeSchema.compatible(dataSchema);
            if (compatible) {
                ConfigMgr mgr = ConfigMgrLoader.load(input);
                ConfigMgr.setMgr(mgr);
            } else {
                throw new SchemaCompatibleException("schema not compatible, ignore load configdata");
            }
        }
    }

    public static void autoReload(ScheduledExecutorService executorService, String fn, Runnable afterReload) throws IOException {
        listen(executorService, Paths.get(fn), () -> {
            try {
                load(fn);
                if (afterReload != null) {
                    afterReload.run();
                }
            } catch (Exception ignored) {
            }
        });
    }

    public static void listen(ScheduledExecutorService executorService, Path path, Runnable callback) throws IOException {
        path = path.toAbsolutePath().normalize();
        if (!path.toFile().isDirectory()) { // 简化
            path = path.getParent();
        }
        Path watchPath = path;
        logger.info("start listen " + watchPath.toFile().getCanonicalPath());
        WatchService ws = watchPath.getFileSystem().newWatchService();
        watchPath.register(ws, StandardWatchEventKinds.ENTRY_MODIFY);
        executorService.scheduleWithFixedDelay(() -> {
            WatchKey key = ws.poll();
            if (key != null) {

                StringBuilder sb = new StringBuilder();
                key.pollEvents().forEach(e ->
                        sb.append("context=").append(e.context()).append(",kind=").append(e.kind()).append(";"));
                logger.info("auto reload " + watchPath + " " + sb.toString());
                callback.run();
                key.reset();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws IOException {
        String fn = "config.bytes";
        load(fn);
        System.out.println(Task.get(1));
        new BytesInspector(fn).match("eq");

        // CodeDataInspector：检查已加载的 ConfigMgr 数据（运行时对象，区别于读 bytes 文件的 BytesInspector）
        CodeDataInspector inspector = new CodeDataInspector(ConfigMgr.getMgr(), ConfigCodeSchema.getCodeSchema());
        CodeDataPrinter printer = new CodeDataPrinter(inspector);
        System.out.println(printer.get("other.monster", "1"));
        System.out.println(printer.get("other.keytest", "1,2"));
        System.out.println(printer.query("1234", ""));
        System.out.println(printer.schema("monster"));
        // printer.loop();

//        ScheduledExecutorService watcher = Executors.newSingleThreadScheduledExecutor();
//        autoReload(watcher, fn, null);
//        System.out.println("read ok");
//        new BytesInspector(fn).loop();
//        watcher.close();
    }
}
