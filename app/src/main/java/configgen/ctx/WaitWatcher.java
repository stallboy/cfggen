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

    public Thread start() {
        return Thread.startVirtualThread(() -> {
            evtVersion = watcher.getEventVersion();
            lastEvtMillis = watcher.getLastEventMillis();
            while (true) {
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
                listener.run();
            }
        }
    }

}
