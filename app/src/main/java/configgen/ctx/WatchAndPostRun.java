package configgen.ctx;

import configgen.gen.Generator;
import configgen.gen.Generators;
import configgen.util.LocaleUtil;
import configgen.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public enum WatchAndPostRun {
    INSTANCE;

    public interface PostRunCallback {
        void onNewContextLoaded(Context newContext);
    }

    private static class PostRunBat {
        String batFile; // .bat 或 .sh 文件
        volatile Thread thread = null; // 执行该bat文件的线程

        PostRunBat(String batFile) {
            this.batFile = batFile;
        }
    }

    private boolean started = false;
    private final List<PostRunBat> postRunBats = new ArrayList<>();
    private final List<PostRunCallback> postRunCallbacks = new ArrayList<>();
    private Context context;

    /**
     * 开始监听，多次调用，只有第一次起效，后面的忽略
     * @param context 上下文
     * @param waitSecondsAfterWatchEvt  监听到文件变化后，等待多少秒再执行reloadData，避免频繁触发
     */
    public void startWatch(Context context, int waitSecondsAfterWatchEvt) {
        if (started) {
            Logger.log(LocaleUtil.getLocaleString("WatchAndPostRun.WatcherAlreadyStarted",
                "file change watcher already started"));
            return;
        }
        if (waitSecondsAfterWatchEvt < 0) {
            Logger.log(LocaleUtil.getLocaleString("WatchAndPostRun.WatcherWaitSecondsInvalid",
                "watcher waitSecondsAfterWatchEvt < 0, ignore start"));
            return;
        }
        this.context = context;
        started = true;

        DirectoryStructure ss = context.sourceStructure();
        Watcher watcher = new Watcher(ss.getRootDir(), ss.getExplicitDir());
        WaitWatcher waitWatcher = new WaitWatcher(watcher, this::reloadData, waitSecondsAfterWatchEvt * 1000);
        waitWatcher.start();
        watcher.start();

        Logger.log(LocaleUtil.getLocaleString("WatchAndPostRun.WatcherStarted",
            "file change watcher started"));
    }

    /**
     * 要在主线程中做注册
     * @param batchFile .bat 或 .sh 文件
     */
    public void registerPostRunBat(String batchFile) {
        if (batchFile == null) {
            return;
        }

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
     * 要在主线程中做注册
     * @param callback 回调函数
     */
    public void registerPostRunCallback(PostRunCallback callback) {
        if (callback == null) {
            return;
        }
        postRunCallbacks.add(callback);
    }

    /**
     * 这是在virtual thread里执行的
     */
    private void reloadData() {
        DirectoryStructure newStructure = context.sourceStructure().reload();
        if (newStructure.lastModifiedEquals(context.sourceStructure())) {
            configgen.util.Logger.verbose(LocaleUtil.getLocaleString("WatchAndPostRun.LastModifiedNotChanged",
                "lastModified not change"));
            return;
        }
        try {
            this.context = new Context(context.contextCfg(), newStructure);
            Logger.log(LocaleUtil.getLocaleString("WatchAndPostRun.ReloadContextOk",
                "reload context ok"));
            onNewContextReloaded();
        } catch (Exception e) {
            Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.ReloadContextIgnored",
                "reload context ignored: {0}", e.getMessage()));
        }

    }

    private void onNewContextReloaded() {
        for (PostRunCallback callback : postRunCallbacks) {
            try {
                callback.onNewContextLoaded(context);
            } catch (Exception e) {
                Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.FailedToRunPostRun",
                    "failed to run post run task: {0}", e.getMessage()));
            }
        }

        for (PostRunBat bat : postRunBats) {
            tryPostRun(bat);
        }

    }


    private void tryPostRun(PostRunBat bat) {
        Thread batThread = bat.thread;
        if (batThread != null) {
            try {
                batThread.join();
            } catch (InterruptedException e) {
                Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.PostRunThreadJoinInterrupted",
                    "post run thread join interrupted: {0}", e.getMessage()));
            }
        }

        String postRun = bat.batFile;
        bat.thread = Thread.startVirtualThread(() -> {
            try {
                String genPrefix = null;
                if (postRun.endsWith(".bat")) {
                    genPrefix = ":: -gen ";
                } else if (postRun.endsWith(".sh")) {
                    genPrefix = "# -gen ";
                }
                if (genPrefix != null) {
                    for (String line : Files.readAllLines(Path.of(postRun))) {
                        if (line.startsWith(genPrefix)) {
                            String parameter = line.substring(genPrefix.length());
                            Generator generator = Generators.create(parameter);
                            if (generator != null) {
                                Logger.log("-gen " + parameter);
                                generator.generate(context);
                            }
                        } else {
                            break;
                        }
                    }
                }

                Process process = Runtime.getRuntime().exec(new String[]{postRun});
                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.PostRunOutput",
                        "post run output: {0}", line));
                }
                if (process.waitFor(10, TimeUnit.SECONDS)) {
                    Logger.log(LocaleUtil.getLocaleString("WatchAndPostRun.PostRunOk",
                        "post run ok!"));
                } else {
                    Logger.log(LocaleUtil.getLocaleString("WatchAndPostRun.PostRunTimeout",
                        "post run timeout"));
                }
                in.close();
            } catch (IOException e) {
                Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.PostRunErr",
                    "post run err: {0}", e.getMessage()));
            } catch (InterruptedException e) {
                Logger.log(LocaleUtil.getFormatedLocaleString("WatchAndPostRun.PostRunInterrupted",
                    "post run interrupted: {0}", e.getMessage()));
            }
        });
    }
}

