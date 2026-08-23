package configgen.ctx;

import configgen.util.Logger;

import java.util.Objects;

/**
 * 监控watcher下的标记，等标记出现且之后waitMillisAfterWatchEvt都不再重复出现后，触发listener
 */
public class WaitWatcher {
    private final Watcher watcher;
    private final Runnable listener;
    private final int waitMillisAfterWatchEvt;
    private final int sleepMillis;

    private long lastEvtMillis;
    private int evtVersion;
    private volatile boolean stopped;
    private Thread startedThread;

    public WaitWatcher(Watcher watcher,
                       Runnable listener,
                       int waitMillisAfterWatchEvt) {
        this(watcher, listener, waitMillisAfterWatchEvt, 100);
    }

    public WaitWatcher(Watcher watcher,
                       Runnable listener,
                       int waitMillisAfterWatchEvt,
                       int sleepMillis) {

        Objects.requireNonNull(watcher);
        Objects.requireNonNull(listener);
        if (waitMillisAfterWatchEvt <= 0) {
            throw new IllegalArgumentException("waitMillisAfterWatchEvt must > 0");
        }
        if (sleepMillis <= 0) {
            throw new IllegalArgumentException("sleepMillis must > 0");
        }
        this.watcher = watcher;
        this.listener = listener;
        this.waitMillisAfterWatchEvt = waitMillisAfterWatchEvt;
        this.sleepMillis = sleepMillis;
    }

    public void start() {
        if (startedThread != null) {
            throw new IllegalStateException("already started");
        }
        stopped = false;
        startedThread = Thread.startVirtualThread(() -> {
            evtVersion = watcher.getEventVersion();
            lastEvtMillis = watcher.getLastEventMillis();
            while (!stopped) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(sleepMillis); // 减少轮询间隔到100ms
                    tick();
                } catch (InterruptedException e) {
                    Logger.log("WaitWatcher stopped by %s", e.toString());
                    return;
                }
            }
        });
    }


    private void tick() {
        int version = watcher.getEventVersion();
        if (evtVersion != version) {
            evtVersion = version;
            lastEvtMillis = watcher.getLastEventMillis(); // 这里跟getEventVersion时机可能不一致，但没关系。
            Logger.verbose2("detected evt");

        } else if (lastEvtMillis > 0) {
            if (System.currentTimeMillis() - lastEvtMillis >= waitMillisAfterWatchEvt) {
                lastEvtMillis = 0;
                try {
                    listener.run();
                } catch (Exception e) {
                    // listener抛异常不能无声杀死轮询线程，否则后续文件变更全部失效
                    Logger.log("WaitWatcher listener err: %s", e.toString());
                }
            }
        }
    }

    /**
     * 支持从listener回调（即轮询线程自身）里调用stop（WatchAndPostRun的autoFix循环保护就是这么用的）：
     * 此时只置停止标志，不能自interrupt+自join——自join要么立刻抛InterruptedException打断上层清理，
     * 要么永久挂死。轮询循环会在本次tick（即当前listener）返回后自行退出。
     */
    public void stop() {
        stopped = true;
        Thread thread = startedThread;
        if (thread == null) {
            return;
        }
        if (thread == Thread.currentThread()) {
            return;
        }
        startedThread = null;
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
