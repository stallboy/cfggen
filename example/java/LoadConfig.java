
import config.ConfigCodeSchema;
import config.ConfigMgr;
import config.ConfigMgrLoader;
import config.task.Task;
import configgen.genjava.*;

import java.io.IOException;
import java.nio.file.*;
//import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LoadConfig {

    private static final Logger logger = Logger.getLogger("LoadConfig");

    public static void load(String fn) throws IOException {
        try (ConfigInput input = new ConfigInput(Path.of(fn))) {
            ConfigPackage pack = ConfigPackage.read(input);
            // Schema/引用校验、扩展初始化全部成功后，才发布新的默认实例。
            ConfigMgr configMgr = ConfigMgrLoader.loadPackagesAndSetMgr(pack);
            System.out.println("config.bytes version=" + ConfigPackage.FORMAT_VERSION + ", metadata=" + pack.metadata());
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

        boolean isRepl = false;
        if (args.length > 0) {
            isRepl = args[0].equals("repl");
        }

        if (isRepl) {
            // CodeDataInspector：检查已加载的 ConfigMgr 数据（运行时对象，区别于读 bytes 文件的 BytesInspector）
            var repl = new Repl("> ");
            CodeDataInspector inspector = new CodeDataInspector(ConfigMgr.getMgr(), ConfigCodeSchema.getCodeSchema());
            CodeDataPrinter printer = new CodeDataPrinter(inspector);
            printer.registerCommands(repl);
            new BytesInspector(fn).registerCommands(repl);
            repl.run();

        } else {

            ConfigMgr original = ConfigMgr.getMgr();
            Task originalTask = original.getTaskTask(1);
            load(fn);
            ConfigMgr reloaded = ConfigMgr.getMgr();
            if (original == reloaded || originalTask == reloaded.getTaskTask(1)) {
                throw new IllegalStateException("Reload must create an independent configuration instance");
            }
            System.out.println("reload: independent instances, original task=" + originalTask);
            System.out.println("task 1 sources=" + reloaded.explain("task.task", 1));

            new BytesInspector(fn).match("eq");
            CodeDataInspector inspector = new CodeDataInspector(ConfigMgr.getMgr(), ConfigCodeSchema.getCodeSchema());
            CodeDataPrinter printer = new CodeDataPrinter(inspector);
            System.out.println(printer.get("other.monster", "1"));
            System.out.println(printer.get("other.keytest", "1,2"));
            System.out.println(printer.query("1234", ""));
            System.out.println(printer.schema("monster"));

            // ScheduledExecutorService watcher = Executors.newSingleThreadScheduledExecutor();
            // autoReload(watcher, fn, null);
            // System.out.println("read ok");
            // new BytesInspector(fn).loop();
            // watcher.close();
        }


    }
}
