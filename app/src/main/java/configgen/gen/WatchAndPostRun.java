package configgen.gen;

import org.jspecify.annotations.Nullable;

import configgen.ctx.Context;
import configgen.layout.DirectoryStructure;
import configgen.ctx.StateCoordinator;
import configgen.ctx.StateCoordinator.EditOutcome;
import configgen.ctx.WaitWatcher;
import configgen.ctx.Watcher;

import configgen.util.LocaleUtil;
import configgen.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public enum WatchAndPostRun {
    INSTANCE;

    private static class PostRunBat {
        String batFile; // .bat 或 .sh 文件
        volatile @Nullable Thread thread = null; // 执行该bat文件的线程

        PostRunBat(String batFile) {
            this.batFile = batFile;
        }
    }

    private boolean started = false;
    // 注册发生在主线程、迭代发生在reload线程，用COW保证可见性
    private final List<PostRunBat> postRunBats = new CopyOnWriteArrayList<>();

    /**
     * 当前代context与其快照订阅的统一协调（见 {@link StateCoordinator} 的说明）：
     * reloadData换代（installState）与所有编辑写操作（runEdit）互斥；
     * server（EditorServer/CfgMcpServer）可同进程并存，写路径都走runEdit天然串行化，
     * 写成功后锁内统一刷新所有订阅者快照，任一server的修改对其他server立即可见。
     */
    private final StateCoordinator<Context> coordinator = new StateCoordinator<>();
    private volatile @Nullable Watcher watcher;
    private @Nullable WaitWatcher waitWatcher;
    // 在 coordinator 的编辑锁内读写，和文件 reload 一起防抖。
    private boolean editPendingPostRun;
    // autoFix写回config.cfg会再触发watch→reload，若对齐不稳定会形成写-触发循环，需要上限保护。
    private final AtomicInteger consecutiveAutoFixReloads = new AtomicInteger();
    private static final int MAX_CONSECUTIVE_AUTO_FIX_RELOADS = 3;
    // 从启动脚本起计时，不依赖 stdout 关闭。
    private static final long POST_RUN_SCRIPT_TIMEOUT_MINUTES = 10;
    private static final long POST_RUN_JOIN_TIMEOUT_MILLIS = 10 * 60 * 1000;

    /**
     * 编辑写操作的统一临界区（EditorServer的record写接口、McpServer的WriteRecordTool都必须经此进入，不得自行取快照）：
     * 与reload换代、与其他写操作互斥；临界区内先执行编辑（写文件+context.updateDataAndValue），
     * 随后统一刷新所有注册快照，保证同进程所有server立刻看到新值。
     */
    public <T> T runEdit(Function<Context, EditOutcome<T>> edit) {
        return coordinator.runEdit(ctx -> {
            EditOutcome<T> outcome = edit.apply(ctx);
            Watcher activeWatcher = watcher;
            if (outcome.committed() && activeWatcher != null && !postRunBats.isEmpty()) {
                editPendingPostRun = true;
                activeWatcher.signalChange();
            }
            return outcome;
        });
    }

    /**
     * 开始监听，多次调用，只有第一次起效，后面的忽略
     * @param waitSecondsAfterWatchEvt  监听到文件变化后，等待多少秒再执行reloadData，避免频繁触发
     */
    public synchronized void startWatch(int waitSecondsAfterWatchEvt) {
        if (started) {
            Logger.log(LocaleUtil.getLocaleString("WatchAndPostRun.WatcherAlreadyStarted",
                "file change watcher already started, shared"));
            return;
        }
        if (waitSecondsAfterWatchEvt == 0) {
            return;
        }
        if (waitSecondsAfterWatchEvt < 0) {
            Logger.log(LocaleUtil.getLocaleString("WatchAndPostRun.WatcherWaitSecondsInvalid",
                "watcher waitSecondsAfterWatchEvt < 0, ignore start"));
            return;
        }
        Context context = Objects.requireNonNull(coordinator.state());
        DirectoryStructure ss = context.sourceStructure();
        Watcher newWatcher = new Watcher(ss.getRootDir());
        WaitWatcher newWaitWatcher = new WaitWatcher(newWatcher, this::reloadData,
                Math.multiplyExact(waitSecondsAfterWatchEvt, 1000));
        consecutiveAutoFixReloads.set(0);
        newWatcher.start();
        try {
            newWaitWatcher.start();
        } catch (RuntimeException | Error e) {
            newWatcher.stop();
            throw e;
        }
        watcher = newWatcher;
        waitWatcher = newWaitWatcher;
        started = true;

        Logger.log(LocaleUtil.getLocaleString("WatchAndPostRun.WatcherStarted",
            "file change watcher started"));
    }

    /**
     * 可能从reloadData（即waitWatcher轮询线程自身）里被调用（autoFix循环保护），WaitWatcher.stop已支持自停。
     */
    private synchronized void stopWatch() {
        if (waitWatcher != null) {
            waitWatcher.stop();
            waitWatcher = null;
        }
        if (watcher != null) {
            watcher.stop();
            watcher = null;
        }
        started = false;
    }

    /**
     * 要在主线程中做注册
     * @param batchFile .bat 或 .sh 文件
     */
    public void registerPostRunBat(String batchFile) {
        for (PostRunBat ob : postRunBats) {
            if (ob.batFile.equals(batchFile)) {
                Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.BatchFileAlreadyRegistered",
                    "batch file {0} already registered for post run", batchFile));
                return;
            }
        }
        postRunBats.add(new PostRunBat(batchFile));
    }

    /**
     * 服务在开始监听前登记。即使未启用 watch，编辑成功后也需要刷新服务快照。
     */
    public synchronized void registerServer(Context context, StateCoordinator.Refresher<Context> refresher) throws IOException {
        Context current = coordinator.state();
        if (current != null && (!current.contextCfg().equals(context.contextCfg())
                || current.usesSharedSchema() != context.usesSharedSchema()
                || (current.usesSharedSchema() && current.cfgSchema() != context.cfgSchema()))) {
            throw new IOException("A server is already registered with a different configuration context: "
                    + current.rootDir());
        }
        coordinator.addRefresher(context, refresher);
    }

    /**
     * 这是在virtual thread里执行的
     */
    private void reloadData() {
        try {
            Context previous = coordinator.state();
            if (!coordinator.reloadState(this::loadChangedContext)) {
                if (consecutiveAutoFixReloads.get() >= MAX_CONSECUTIVE_AUTO_FIX_RELOADS) {
                    stopWatch();
                }
                return;
            }
            if (coordinator.state() != previous) {
                Logger.log(LocaleUtil.getLocaleString("WatchAndPostRun.ReloadContextOk",
                    "reload context ok"));
            }

            for (PostRunBat bat : postRunBats) {
                tryPostRun(bat);
            }
        } catch (Exception e) {
            Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.ReloadContextIgnored",
                "reload context ignored: {0}", e.toString()));
            if (Logger.verboseLevel() > 0) {
                StringWriter stackTrace = new StringWriter();
                e.printStackTrace(new PrintWriter(stackTrace));
                Logger.log(stackTrace.toString().stripTrailing());
            }
        }
    }

    private @Nullable Context loadChangedContext(Context current) {
        DirectoryStructure source = current.sourceStructure();
        DirectoryStructure discovered = source.reload();
        if (discovered.lastModifiedEquals(source)) {
            if (editPendingPostRun) {
                // 写回已更新内存和文件时间戳，脚本可以直接使用当前上下文。
                editPendingPostRun = false;
                return current;
            }
            Logger.verbose(LocaleUtil.getLocaleString("WatchAndPostRun.LastModifiedNotChanged",
                    "lastModified not change"));
            return null;
        }

        Context.LoadResult loaded = current.reloadFrom(discovered);
        if (!loaded.schemaRewritten()) {
            consecutiveAutoFixReloads.set(0);
            editPendingPostRun = false;
            return loaded.context();
        }

        int reloads = consecutiveAutoFixReloads.incrementAndGet();
        if (reloads < MAX_CONSECUTIVE_AUTO_FIX_RELOADS) {
            editPendingPostRun = false;
            return loaded.context();
        }
        Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.AutoFixLoopDetected",
                "reload触发autoFix写回config.cfg已连续{0}次，疑似对齐不稳定导致写-触发循环，停止watch（保留当前内存状态）",
                reloads));
        return null;
    }

    private void tryPostRun(PostRunBat bat) {
        Thread batThread = bat.thread;
        if (batThread != null) {
            try {
                batThread.join(POST_RUN_JOIN_TIMEOUT_MILLIS);
            } catch (InterruptedException e) {
                Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.PostRunThreadJoinInterrupted",
                    "post run thread join interrupted: {0}", e.getMessage()));
            }
            if (batThread.isAlive()) {
                // 上一个post run卡住（如外部进程不关stdout导致readLine永久阻塞），
                // 跳过本次触发，不能让reload管道跟着永久停摆
                Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.PostRunStillRunning",
                    "previous post run still running after {0}ms, skip this trigger: {1}",
                    POST_RUN_JOIN_TIMEOUT_MILLIS, bat.batFile));
                return;
            }
        }

        String postRun = bat.batFile;
        bat.thread = Thread.startVirtualThread(() -> {
            try {
                // 内嵌生成和它启动的外部脚本属于同一个串行任务。
                synchronized (GenerationBatch.class) {
                    executePostRun(postRun);
                }
            } catch (IOException | RuntimeException e) {
                Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.PostRunErr",
                    "post run err: {0}", e.getMessage()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.PostRunInterrupted",
                    "post run interrupted: {0}", e.getMessage()));
            }
        });
    }

    private void executePostRun(String postRun) throws IOException, InterruptedException {
        String genPrefix = null;
        if (postRun.endsWith(".bat")) {
            genPrefix = ":: -gen ";
        } else if (postRun.endsWith(".sh")) {
            genPrefix = "# -gen ";
        }
        if (genPrefix != null) {
            Context runCtx = java.util.Objects.requireNonNull(coordinator.state());
            List<Generator> generators = new ArrayList<>();
            for (String line : Files.readAllLines(Path.of(postRun))) {
                if (!line.startsWith(genPrefix)) {
                    break;
                }
                String parameter = line.substring(genPrefix.length());
                Generator generator = Generators.create(parameter);
                if (generator != null) {
                    Logger.log("-gen " + parameter);
                    generators.add(generator);
                }
            }
            GenerationBatch.run(runCtx, generators);
        }

        Process process = new ProcessBuilder(postRun).redirectErrorStream(true).start();
        // 独立读取 stdout，避免脚本不关闭输出时一直占用生成入口。
        Thread output = Thread.startVirtualThread(() -> logPostRunOutput(process));
        try {
            if (!process.waitFor(POST_RUN_SCRIPT_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                Logger.log(LocaleUtil.getLocaleString("WatchAndPostRun.PostRunTimeout", "post run timeout"));
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly().waitFor();
                }
                return;
            }

            output.join(TimeUnit.SECONDS.toMillis(5));
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                Logger.log(LocaleUtil.getLocaleString("WatchAndPostRun.PostRunOk", "post run ok!"));
            } else {
                Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.PostRunExitCode",
                    "post run failed (exit code {0}): {1}", exitCode, postRun));
            }
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly().waitFor();
            }
        }
    }

    private static void logPostRunOutput(Process process) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.PostRunOutput",
                    "post run output: {0}", line));
            }
        } catch (IOException e) {
            Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.PostRunErr",
                "post run err: {0}", e.getMessage()));
        }
    }
}
