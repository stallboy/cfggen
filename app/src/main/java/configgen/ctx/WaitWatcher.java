package configgen.ctx;

import configgen.util.Logger;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

public class WaitWatcher {
    private final Watcher watcher;
    private final Runnable listener;
    private final int waitSecondsAfterWatchEvt;

    private long lastEvtSec;

    public WaitWatcher(Watcher watcher,
                       Runnable listener,
                       int waitSecondsAfterWatchEvt) {

        Objects.requireNonNull(watcher);
        Objects.requireNonNull(listener);
        if (waitSecondsAfterWatchEvt <= 0) {
            throw new IllegalArgumentException("waitSecondsAfterWatchEvt must >= 0");
        }
        this.watcher = watcher;
        this.listener = listener;
        this.waitSecondsAfterWatchEvt = waitSecondsAfterWatchEvt;
    }

    public Thread start() {
        return Thread.startVirtualThread(() -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    watchLoop();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    private void watchLoop() throws InterruptedException {
        long evtSec = watcher.getLastEvtSecAndReset();
        if (evtSec > 0) {
            lastEvtSec = evtSec;
            Logger.verbose2("detected evt");

        } else if (lastEvtSec > 0) {
            if (Instant.now().getEpochSecond() - lastEvtSec >= waitSecondsAfterWatchEvt) {
                lastEvtSec = 0;
                listener.run();
            }
        }
        Thread.sleep(1000);
    }

}
