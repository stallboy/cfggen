
import config.ConfigCodeSchema;
import config.ConfigMgr;
import config.ConfigMgrLoader;
import config.equip.Rank;
import config.other.Monster;
import config.task.Task;
import config.task.completecondition.KillMonster;
import configgen.genjava.*;

import java.io.IOException;
import java.nio.file.*;
//import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LoadConfig {

    private static final Logger logger = Logger.getLogger("LoadConfig");

    public static ConfigPackage load(String fn) throws IOException {
        ConfigPackage pack = ConfigPackage.read(Path.of(fn));
        // Schema/引用校验、扩展初始化全部成功后，才发布新的默认实例。
        ConfigMgrLoader.loadPackagesAndSetMgr(pack);
        System.out.println("base.bytes version=" + ConfigPackage.FORMAT_VERSION + ", metadata=" + pack.metadata());
        return pack;
    }

    private static void showMod(ConfigPackage base) throws IOException {
        ConfigMgr original = ConfigMgr.getMgr();
        ConfigPackage mod = ConfigPackage.read(Path.of("demo-mod.bytes"));
        ConfigMgr modified = ConfigMgrLoader.loadPackages(base, mod);
        ConfigMgr.setMgr(modified);
        Task hunt = Task.get(1001);
        var condition = (KillMonster) hunt.getCompletecondition();

        System.out.println("\n=== Mod example ===");
        System.out.println("monster 1: base x=" + original.getOtherMonster(1).getPosList().getFirst().getX()
                + ", mod x=" + Monster.get(1).getPosList().getFirst().getX());
        System.out.println("monster 2 unchanged: x=" + Monster.get(2).getPosList().getFirst().getX());
        System.out.println("monster 1001: base=" + original.getOtherMonster(1001) + ", mod=" + Monster.get(1001));
        System.out.println("green: base=" + Rank.GREEN.ref(original).getRankShowName() + ", mod=" + Rank.GREEN.getRankShowName());
        System.out.println("task 1001: " + hunt.getName() + ", target monster=" + condition.refMonsterid().getId()
                + ", exp=" + hunt.getExp());
        System.out.println("monster 1 sources=" + modified.explain("other.monster", 1));
        System.out.println("monster 1001 sources=" + modified.explain("other.monster", 1001));
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
        String fn = "base.bytes";
        ConfigPackage base = load(fn);
        showMod(base);
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

            new BytesInspector(fn).match("eq");
            CodeDataInspector inspector = new CodeDataInspector(ConfigMgr.getMgr(), ConfigCodeSchema.getCodeSchema());
            CodeDataPrinter printer = new CodeDataPrinter(inspector);
            System.out.println(printer.get("other.monster", "1"));
            System.out.println(printer.get("other.key_test", "1,2"));
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
